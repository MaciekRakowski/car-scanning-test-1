package com.example.maciek.testapplication2;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.graphics.*;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Intent;

import java.io.FileDescriptor;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.bees4honey.vinscanner.B4HScanner;


/**
 * Wrapper Activity for native VIN scanner library. Activity can function in two modes: single scan and continuous scan.
 * In single mode the first scanned VIN-code is returned in intent.
 * In continuous mode the scanned code is shown to user and then scanner continues the scanning process.
 * Code to start Scanner activity in single mode:
 *
 * <pre>
 * {@code
 * Intent intent = new Intent(curContext, com.bees4honey.vinscanner.Scanner.class);
 * intent.putExtra(com.bees4honey.vinscanner.Scanner.SINGLE_SCAN, true);
 * startActivityForResult(intent, SCAN_CODE);
 * }
 * </pre>
 *
 * Code to get scanned VIN if scanner was started in single mode:
 *
 * <pre>
 * {@code
 * protected void onActivityResult(int requestCode, int resultCode, Intent data) {
 *     if (requestCode == SCAN_CODE && resultCode == RESULT_OK) {
 *         String scannedVIN = data.getStringExtra(com.bees4honey.vinscanner.Scanner.SCANNED_CODE);
 *         Log.d("VinScanner", "Scanned vin: " + scannedVIN);
 *     }
 * }
 * }
 * </pre>
 */
public class Scanner extends Activity implements SurfaceHolder.Callback {
    /**
     * Key for boolean extra which should be passed to the activity. If true then first scanned VIN code would be passed
     * back to the calling activity with intent containing {@link #SCANNED_CODE} activity. If no value passed with SINGLE_SCAN value
     * then scanner is functioning in continuous mode.
     */
    public static final String SINGLE_SCAN = "single_scan";

    /**
     * A key for scanned code returned from activity if it was started in single scan mode.
     */
    public static final String SCANNED_CODE = "scanned_code";

    private static final String TAG = "VinScanner";
    private static final int AUTOFOCUS_DELAY = 500;
    private static final int AUTOFOCUS_TIMEOUT = 1500;
    private static final int IDX_BEEP = 0;
    private static final int IDX_VIBRATE = 1;

    private static final int QUIT = 0x7f060003;
    private static final int DECODE = 2131099648;
    private static final int DECODED = 0x7f060003;
    private static final int FOCUSED = 0x7f060001;
    private static final int TORCH_ON_OFF = 0x7f060009;

    private volatile Camera camera;
    private final Camera.AutoFocusCallback cameraAutoFocusCallback;
    private CountDownLatch cameraLatch;
    CameraThread cameraThread;
    private final B4HScanner b4HScanner;
    Handler handler;
    private SurfaceHolder holder;
    private long lastAutofocus;
    private MediaPlayer mediaPlayer;
    private Camera.Size previewSize;
    private boolean previewing;
    private boolean scanning;
    private boolean settings[] = {true, true};
    private boolean surfaceChangedDelayed;
    private ViewFinder finderView;
    private final Runnable watchdog;
    private ImageButton buttonTorchOnOff;
    private RotatableTextView vincodeView;
    private ImageView aim2DView;
    private SupportedOrientations curOrientation;    //current orientation of device.
    private OrientationEventListener orientationListener;
    private boolean singleScan; //a flag which shows if scanned code should be returned immediately.

    //scanning can be performed both in horizontal and vertical directions. Flag shows the direction of scanning
    private boolean scanHorizontally;

    //Button to control scanning direction
    private ImageButton buttonScanDirection;

    private TorchControl torchControl;
    private boolean torchIsOn;

    private final Camera.PreviewCallback cameraPreviewCallback;
    Toast vincode;

    // constructor
    public Scanner() {
        super();

        // initialize class objects
        previewing = false;
        scanning = false;
        lastAutofocus = 0;
        surfaceChangedDelayed = false;

        // object for handling messages
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case DECODE:
                        // message called when getting image from camera
                        Scanner.this.decode((byte[]) msg.obj);
                        break;

                    case FOCUSED:
                        // message called when autofocusing
                        Scanner.this.lastAutofocus = System.currentTimeMillis() - AUTOFOCUS_TIMEOUT + AUTOFOCUS_DELAY;
                        break;

                    case TORCH_ON_OFF:
                        Log.i(TAG, "Torch On/Off message received. Command: " + msg.what);
                        Scanner.this.camera.setPreviewCallback(null);

                        if (Scanner.this.torchControl != null) {
                            Scanner.this.torchControl.torch(!Scanner.this.torchControl.isEnabled());
                            break;
                        }

                        Camera.Parameters CameraParameters = Scanner.this.camera.getParameters();
                        if (CameraParameters.getFlashMode().compareTo(android.hardware.Camera.Parameters.FLASH_MODE_TORCH) == 0) {
                            Log.i(TAG, "Torch Off");
                            torchIsOn = false;
                            int resourceId = getApplication().getResources().getIdentifier("light", "drawable", getApplication().getPackageName());
                            buttonTorchOnOff.setImageBitmap(getRotatedBitmapForDrawable(resourceId));
                            CameraParameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_OFF);
                        } else {
                            Log.i(TAG, "Torch On");
                            int resourceId = getApplication().getResources().getIdentifier("light_on", "drawable", getApplication().getPackageName());
                            buttonTorchOnOff.setImageBitmap(getRotatedBitmapForDrawable(resourceId));
                            torchIsOn = true;
                            CameraParameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_TORCH);
                        }

                        // refresh camera settings
                        try {
                            Scanner.this.camera.setParameters(CameraParameters);
                        } catch (Exception e) {
                            Log.e(TAG, "Torch On/Off Exception", e);
                            Message nextmsg = handler.obtainMessage(TORCH_ON_OFF);
                            sendMessageDelayed(nextmsg, 100);
                            Log.e(TAG, "send message");
                        }
                        break;

                    case DECODED:
                        // message called when VIN ode is obtained successfully
                        // continue scanning
                        setScanning(true);
                }

                // call parent method to process any other messages
                super.handleMessage(msg);
            }
        };

        // object of processing event when camera is autofocused
        cameraAutoFocusCallback = new android.hardware.Camera.AutoFocusCallback() {
            public void onAutoFocus(boolean success, Camera camera) {
                // message is sent when autofocused
                Scanner.this.handler.sendEmptyMessageDelayed(FOCUSED, 500);
            }
        };

        // object for processing event when image is obtained from camera
        cameraPreviewCallback = new Camera.PreviewCallback() {
            public void onPreviewFrame(byte[] data, Camera camera) {
                // if scanning is on
                if (isScanning()) {
                    // then message is sent containing picture
                    Message msg = Scanner.this.handler.obtainMessage(DECODE, data);
                    Scanner.this.handler.sendMessage(msg);
                }
            }
        };

        // object which calls itself in defined time and intended
        // for restarting autofocus and getting image for processing
        watchdog = new Runnable() {
            public void run() {
                handler.postDelayed(watchdog, 100);

                // get current time
                long currentTime = java.lang.System.currentTimeMillis();

                if (previewing) {
                    if (isScanning()) {
                        // programm is in PREVIEW and SCANNING mode
                        // If enough time is gone since last autofocus
                        if (currentTime - lastAutofocus > AUTOFOCUS_TIMEOUT) {    // then restart autofocus
                            lastAutofocus = currentTime;
                            camera.autoFocus(cameraAutoFocusCallback);
                        }

                        // if there is no message about processing image in queue
                        if (!handler.hasMessages(DECODE)) {
                            // then register handler on getting image
                            camera.setOneShotPreviewCallback(cameraPreviewCallback);
                        }
                    }
                }
            }
        };

        // create and initialize object with native code
        b4HScanner = new B4HScanner();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // change parameters of displayed window
        // app is shown on full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        singleScan = getIntent().getBooleanExtra(SINGLE_SCAN, false);

        // bind displaying context
        int scanId = getApplication().getResources().getIdentifier("scan", "layout", getApplication().getPackageName());
        setContentView(scanId);

        //Activity is only enabled for landscape layout.
        //Default landscape angle diff from default portrait orientation is 270 degrees
        curOrientation = SupportedOrientations.LANDSCAPE;
        //register orientation listener to handle orientation changes
        orientationListener = new ScannerOrientationListener(this);

        // init local variables for fast access to displayed elements
        int resourceId = getApplication().getResources().getIdentifier("viewfinder_view", "id", getApplication().getPackageName());
        finderView = (ViewFinder) findViewById(resourceId);
        resourceId = getApplication().getResources().getIdentifier("tv_vincode", "id", getApplication().getPackageName());
        vincodeView = (RotatableTextView) findViewById(resourceId);
        resourceId = getApplication().getResources().getIdentifier("image_2d_aim", "id", getApplication().getPackageName());
        aim2DView = (ImageView) findViewById(resourceId);

        try {
            torchControl = new TorchControl();
        } catch (Exception e) {
            torchControl = null;
        }

        resourceId = getApplication().getResources().getIdentifier("TorchButton", "id", getApplication().getPackageName());
        buttonTorchOnOff = (ImageButton) this.findViewById(resourceId);
        buttonTorchOnOff.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Message msg = handler.obtainMessage(TORCH_ON_OFF);
                handler.sendMessage(msg);
            }
        });

        scanHorizontally = true;
        resourceId = getApplication().getResources().getIdentifier("orientationButton", "id", getApplication().getPackageName());
        buttonScanDirection = (ImageButton) findViewById(resourceId);
        buttonScanDirection.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeScanningDirection();
            }
        });

        // init player for sound and vibrate when VIN scan is successful
        initBeep();
    }

    private void changeScanningDirection() {
        scanHorizontally = !scanHorizontally;
        ViewFinder.ContentOrientation orientation = scanHorizontally ?
                ViewFinder.ContentOrientation.HORIZONTAL :
                ViewFinder.ContentOrientation.VERTICAL;
        finderView.setContentOrientation(orientation);
    }

    // Play sound and vibrate when VIN scan is successful
    public void beepAndVibrate() {
        if (mediaPlayer != null)        // check player
        {
            if (settings[IDX_BEEP]) {
                mediaPlayer.start();    // play sound
            }
        }
        if (settings[IDX_VIBRATE]) {
            Vibrator vibr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibr.vibrate(200);            // vibrate
        }
    }

    // decode obtained image
    private void decode(byte[] data) {
        if (previewing) {
            if (isScanning()) {    // when program is in preview and scanning, then CallBack is set to receive one frame
                camera.setOneShotPreviewCallback(cameraPreviewCallback);
            }
        }

        if (data != null) {
            if (isScanning()) {
                // there is data on input and program is in scanning mode

                setScanning(false);                // temporary turn off scanning

                byte[] newData = data;
                //scanning direction has been changed so we should rotate the image we received from camera
                int scanW = previewSize.width;
                int scanH = previewSize.height;
                if (!scanHorizontally) {
                    //rotate image on 90 degrees
                    scanW = previewSize.height;
                    scanH = previewSize.width;
                    newData = rotateCameraImage(data, previewSize.width, previewSize.height);
                }

                // call native procedure to detect VIN
                String decodedVIN = null;
                int sum = 0;
                for(int i = 0; i < Math.min(100, newData.length); i++) {
                    sum += newData[i];
                }
                if (sum > 0) {
                    decodedVIN = b4HScanner.parse(newData, scanW, scanH, this);
                }

                    if (decodedVIN != null) {
                    // decodedVIN contains line with read code

                    beepAndVibrate();    // play sound and vibrate

                    if (singleScan) {
                        //Finish scanning and return scanned code.
                        finishScan((String) decodedVIN.subSequence(0, decodedVIN.length()));
                    } else {
                        // show decoded vin and continue scanning
                        showVinCode(decodedVIN);
                    }

                    // send message to another thread that code is obtained
                    Message msg = handler.obtainMessage(DECODED, decodedVIN);
                    handler.sendMessageDelayed(msg, 4600);
                } else {
                    setScanning(true);        // restore scanning mode
                }
            }
        }
    }

    // Finish scan and return result to the calling activity.
    public void finishScan(String codeResult) {
        Intent intent = getIntent();
        intent.putExtra(Scanner.SCANNED_CODE, codeResult);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void showVinCode(String vincode) {
        vincodeView.setText(vincode);
        vincodeView.setVisibility(View.VISIBLE);

        EnterVin.mVin = vincode;
        Intent intent = new Intent(this, ActivityMainPageViewer.class);
        this.startActivity(intent);
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.putExtra("vin", vincode);
//        this.startActivity(intent);
    }

    private byte[] rotateCameraImage(byte[] data, int width, int height) {
        byte[] rotatedData = new byte[data.length];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++)
                rotatedData[x * height + height - y - 1] = data[x + y * width];
        }
        return rotatedData;
    }

    // initialize mediaPlayer and set sound file
    private void initBeep() {
        if (mediaPlayer == null) {
            setVolumeControlStream(3);
            mediaPlayer = new MediaPlayer();    // create mediaPlayer object
            mediaPlayer.setAudioStreamType(3);// set type of played file

            // played file is in resources
            Resources res = getResources();

            try {
                int resourceId = getApplication().getResources().getIdentifier("scanned", "raw", getApplication().getPackageName());
                AssetFileDescriptor assetFileDescriptor = res.openRawResourceFd(resourceId);
                // get file descriptor
                FileDescriptor fileDescriptor = assetFileDescriptor.getFileDescriptor();

                // get beginning of file
                long startOffset = assetFileDescriptor.getStartOffset();

                // get length of file
                long lenght = assetFileDescriptor.getLength();

                // set played data in mediaPlayer
                mediaPlayer.setDataSource(fileDescriptor, startOffset, lenght);

                // close descriptor of file
                assetFileDescriptor.close();

                // set volume of playback
                mediaPlayer.setVolume(1.0f, 1.0f);
                mediaPlayer.prepare();
            } catch (Exception e) {
                // in case of any error release the object
                Log.i(TAG, "Error of media player init: " + e.getMessage());
                mediaPlayer = null;
            }
        }
    }

    // returns SCANNING flag
    private boolean isScanning() {
        return scanning;
    }

    // Set scanning mode
    private void setScanning(boolean scanFlag) {
        scanning = scanFlag;

        // In dependance of transfered parameter, mode of displaying is switched
        finderView.setRunning(scanning);
        vincodeView.setVisibility(View.INVISIBLE);
    }

    // Starts Preview
    void cameraStartPreview(android.view.SurfaceHolder pholder, int width, int height) {
        Log.i(TAG, "Camera Start Preview");
        try {
            String str = "flash-value";

            // get object with camera parameters
            Camera.Parameters cameraParameters = camera.getParameters();

            try {
                List<String> supportedFlash = cameraParameters.getSupportedFlashModes();
                if (torchControl == null && (supportedFlash == null ||
                        !supportedFlash.contains(android.hardware.Camera.Parameters.FLASH_MODE_ON)))
                    throw new Exception();
            } catch (Exception e) {
                buttonTorchOnOff.setVisibility(View.INVISIBLE);
            }

            // set size of captured image
            String s = "Start preview: width= " + width + "height= " + height;
            Log.i(TAG, s);

            List<Size> sizes = cameraParameters.getSupportedPreviewSizes();
            Size optimalSize = getOptimalPreviewSize(sizes, width, height);
            Log.d(TAG, "Optimal preview size is chosen: width=" + optimalSize.width + " height=" + optimalSize.height);
            cameraParameters.setPreviewSize(optimalSize.width, optimalSize.height);

            // set YUV data format.
            cameraParameters.setPreviewFormat(ImageFormat.NV21);

            // set frequency of capture
            configureFrameRate(cameraParameters);

            if (cameraParameters.get(str) != null) {
                cameraParameters.set(str, 2);
            }

            // turn off flash
            if (cameraParameters.getFlashMode() != null) {
                if (torchControl != null)
                    torchControl.torch(false);
                else
                    cameraParameters.set("flash-mode", android.hardware.Camera.Parameters.FLASH_MODE_OFF);
            }

            // set focus mode
            cameraParameters.set("focus-mode", android.hardware.Camera.Parameters.FOCUS_MODE_AUTO);

            // refresh camera settings
            camera.setParameters(cameraParameters);
            camera.setDisplayOrientation(calculateCameraDisplayOrientation(Camera.CameraInfo.CAMERA_FACING_BACK));
            previewSize = cameraParameters.getPreviewSize();

            // turn off handler when getting image, as far as image
            // should only be get when autofocus
            camera.setPreviewCallback(null);
            //camera.setDisplayOrientation(0);
            // define where to show preview
            camera.setPreviewDisplay(pholder);

            // Starts Preview
            camera.startPreview();
            previewing = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    private void configureFrameRate(Camera.Parameters params) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            List<int[]> ranges = params.getSupportedPreviewFpsRange();
            int []selectedRange = {0, 0};
            if (ranges != null) {
                for (int[] range : ranges) {
                    if (range[0] > selectedRange[0]) {
                        selectedRange[0] = range[0];
                        selectedRange[1] = range[1];
                    }
                }
                params.setPreviewFpsRange(selectedRange[0], selectedRange[1]);
            }
        } else {
            params.setPreviewFrameRate(15);
        }
    }

    private Size getOptimalPreviewSize(List<Size> sizes, int targetWidth, int targetHeight) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) targetWidth / targetHeight;
        if (sizes == null)
            return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private int calculateCameraDisplayOrientation(int cameraId) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    // Stops Preview
    void cameraStopPreview() {
        if (previewing) {
            previewing = false;                    // erase flag
            camera.setPreviewCallback(null);    // erase all handlers
            camera.stopPreview();                // stop preview in camera
        }
    }

    // handler of buttons pushes
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == 80 || keyCode == 27)
            return true;
        if (keyCode == 82)
            openOptionsMenu();    // if "Menu" is pushed then open menu
        else
            return super.onKeyDown(keyCode, event);        // call of parent handler
        return true;
    }

    // method called on stop/pause (e.g. when launching browser)
    @Override
    protected void onPause() {
        previewing = false;        // erase flag for PREVIEW mode
        camera.setPreviewCallback(null);    // erase all handlers
        camera.stopPreview();                // stop preview in camera

        if (cameraThread != null) {    // if camera object is initialized
            cameraThread.finish();        // finish camera thread
            cameraThread = null;        // reset thread object

            try {
                Thread.sleep(200);        // to avoid thread race
            } catch (Exception e) {
            }
        }

        //disabling orientation listener to save resources
        orientationListener.disable();

        Log.i(TAG, "torch mode control");

        // turn off torch mode
        if (torchControl != null)
            torchControl.torch(false);
        else {
            try {
                Camera.Parameters cameraParameters = camera.getParameters();
                if (cameraParameters.getFlashMode() != null &&
                        (cameraParameters.getFlashMode().compareTo(android.hardware.Camera.Parameters.FLASH_MODE_TORCH) == 0 ||
                                cameraParameters.getFlashMode().compareTo(android.hardware.Camera.Parameters.FLASH_MODE_ON) == 0)) {
                    Log.i(TAG, "Torch Off");
                    int resourceId = getApplication().getResources().getIdentifier("light", "drawable", getApplication().getPackageName());
                    buttonTorchOnOff.setImageBitmap(getRotatedBitmapForDrawable(resourceId));
                    cameraParameters.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_OFF);
                    torchIsOn = false;

                    // refresh camera settings
                    camera.setParameters(cameraParameters);
                }
            } catch (Exception e) {
                Log.e(TAG, "torch On/Off Exception", e);
            }
        }

        // remove object from schedule
        handler.removeCallbacks(watchdog);

        setScanning(false);        // stop scanning mode
        camera.release();        // release camera for another apps

        if (vincode != null)
            vincode.cancel();

        // call handler of parent class
        super.onPause();
    }

    // handler called at start/restore of the app
    @Override
    protected void onResume() {
        super.onResume();    // call method of parent class
        getWindow().addFlags(128);

        if (orientationListener.canDetectOrientation()) {
            orientationListener.enable();
        }

        // start camera thread
        camera = Camera.open();

        cameraLatch = new CountDownLatch(1);
        cameraThread = new CameraThread(this);
        cameraThread.start();

        try {
            cameraLatch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }

        lastAutofocus = 0;            // reset last autofocus time
        previewing = false;            // turn off preview(with camera)
        setScanning(true);            // app goes to scanning mode immediately
        watchdog.run();                // start task on schedule

        int resourceId = getApplication().getResources().getIdentifier("light", "drawable", getApplication().getPackageName());
        buttonTorchOnOff.setImageBitmap(getRotatedBitmapForDrawable(resourceId));
        torchIsOn = false;

        if (holder == null) {
            // if app is just launched
            // get holder to display PREVIEW
            resourceId = getApplication().getResources().getIdentifier("camera_view", "id", getApplication().getPackageName());
            SurfaceView view = (SurfaceView) findViewById(resourceId);
            SurfaceHolder surfaceHolder = view.getHolder();

            // set type of displaying image
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            // set event handler
            surfaceHolder.addCallback(this);
        } else {
            // If app is getting restored
            if (!surfaceChangedDelayed) {
                // if there is a delayed event
                surfaceChangedDelayed = false;    // reset flag of delayed event

                // restore holder work
                resourceId = getApplication().getResources().getIdentifier("camera_view", "id", getApplication().getPackageName());
                SurfaceView view = (SurfaceView) findViewById(resourceId);
                surfaceChanged(holder, PixelFormat.OPAQUE, view.getWidth(), view.getHeight());
            }
        }
    }

    // called when creating/restoring app window
    @Override
    public void surfaceChanged(SurfaceHolder pholder, int format, int width, int height) {

        if (camera != null) {
            try {
                camera.stopPreview();    // stop camera preview

                // restart camera preview
                String s = "width = " + width + " height = " + height;
                if (pholder == null)
                    Log.i(TAG, "!!! Pholder = NULL");
                Log.i(TAG, s);
                cameraStartPreview(pholder, width, height);
            } catch (Exception e) {
                Log.i(TAG, "exception raised while working with camera: " + e.getMessage());
                surfaceChangedDelayed = true;
            }
        } else {
            // if camera object is not created, then processing is delayed
            surfaceChangedDelayed = true;
        }
    }

    // called at creating app window
    @Override
    public void surfaceCreated(SurfaceHolder pholder) {
        this.holder = pholder;
        surfaceChangedDelayed = false;
    }

    // called at delete app window
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        this.holder = null;
        cameraStopPreview();    // stop preview
    }

    private void rearrangeControls(SupportedOrientations newOrientation) {
        if (curOrientation == newOrientation) {
            //rearrange view only if orientation changed
            return;
        }

        if (curOrientation.isLandscape() != newOrientation.isLandscape()) {
            changeScanningDirection();
        }

        curOrientation = newOrientation;
        int resourceId = getApplication().getResources().getIdentifier("rotate", "drawable", getApplication().getPackageName());
        Bitmap bm = BitmapFactory.decodeResource(getResources(), resourceId);
        bm = rotateBitmapForCurrentOrientation(bm);
        buttonScanDirection.setImageBitmap(bm);

        updateVinViewOrientation();

        if (torchIsOn) {
            resourceId = getApplication().getResources().getIdentifier("light_on", "drawable", getApplication().getPackageName());
            buttonTorchOnOff.setImageBitmap(getRotatedBitmapForDrawable(resourceId));
        } else {
            resourceId = getApplication().getResources().getIdentifier("light", "drawable", getApplication().getPackageName());
            buttonTorchOnOff.setImageBitmap(getRotatedBitmapForDrawable(resourceId));
        }
    }

    private void updateVinViewOrientation() {
        int resourceId;
        switch (curOrientation) {
            case PORTRAIT:
                resourceId = getApplication().getResources().getIdentifier("aim_2d_rotated270", "drawable", getApplication().getPackageName());
                vincodeView.setTextOrientation(RotatableTextView.TextOrientations.DOWN_TOP);
                aim2DView.setBackgroundResource(resourceId);
                break;
            case UPSIDE_DOWN:
                resourceId = getApplication().getResources().getIdentifier("aim_2d_rotated90", "drawable", getApplication().getPackageName());
                vincodeView.setTextOrientation(RotatableTextView.TextOrientations.TOP_DOWN);
                aim2DView.setBackgroundResource(resourceId);
                break;
            case LANDSCAPE:
                resourceId = getApplication().getResources().getIdentifier("aim_2d", "drawable", getApplication().getPackageName());
                vincodeView.setTextOrientation(RotatableTextView.TextOrientations.NORMAL);
                aim2DView.setBackgroundResource(resourceId);
                break;
            case LANDSCAPE_REVERSE:
                resourceId = getApplication().getResources().getIdentifier("aim_2d_rotated180", "drawable", getApplication().getPackageName());
                vincodeView.setTextOrientation(RotatableTextView.TextOrientations.REVERSE);
                aim2DView.setBackgroundResource(resourceId);
                break;
            default:
                //do nothing
        }
    }

    private Bitmap getRotatedBitmapForDrawable(int drawable) {
        Bitmap bm = BitmapFactory.decodeResource(getResources(), drawable);
        return rotateBitmapForCurrentOrientation(bm);
    }

    private Bitmap rotateBitmapForCurrentOrientation(Bitmap source) {
        float angle = .0f;
        switch (curOrientation) {
            case PORTRAIT:
                angle = 270.0f;
                break;
            case LANDSCAPE_REVERSE:
                angle = 180.0f;
                break;
            case UPSIDE_DOWN:
                angle = 90.0f;
                break;
            case LANDSCAPE:
                angle = 0.0f;
                break;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(angle);

        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    /**
     * **********************************************************************
     */

    //Class to control orientation changes of device
    private class ScannerOrientationListener extends OrientationEventListener {
        private static final int ROTATION_THRESHOLD = 30;
        private SupportedOrientations curOrientation;

        public ScannerOrientationListener(Context context) {
            super(context, SensorManager.SENSOR_DELAY_NORMAL);
            curOrientation = SupportedOrientations.LANDSCAPE;
        }

        @Override
        public void onOrientationChanged(int orientationInDegrees) {
            if (orientationInDegrees == ORIENTATION_UNKNOWN) {
                //do nothing
                return;
            }

            SupportedOrientations newOrientation = getRoughScreenOrientation(orientationInDegrees);
            if (curOrientation == newOrientation) {
                //do something only if orientation changed
                return;
            }

            curOrientation = newOrientation;
            rearrangeControls(curOrientation);
        }

        private SupportedOrientations getRoughScreenOrientation(int orientationInDegrees) {
            SupportedOrientations newOrientation = curOrientation;
            if (orientationInDegrees < ROTATION_THRESHOLD || orientationInDegrees > 360 - ROTATION_THRESHOLD) {
                // normal (portrait).
                newOrientation = SupportedOrientations.PORTRAIT;
            } else if (orientationInDegrees < 270 + ROTATION_THRESHOLD && orientationInDegrees > 270 - ROTATION_THRESHOLD) {
                // right side at the top
                newOrientation = SupportedOrientations.LANDSCAPE;
            } else if (orientationInDegrees < 90 + ROTATION_THRESHOLD && orientationInDegrees > 90 - ROTATION_THRESHOLD) {
                // left side at the top
                newOrientation = SupportedOrientations.LANDSCAPE_REVERSE;
            } else if (orientationInDegrees < 180 + ROTATION_THRESHOLD && orientationInDegrees > 180 - ROTATION_THRESHOLD) {
                // orientation is upside down (portrait)
                newOrientation = SupportedOrientations.UPSIDE_DOWN;
            }

            return newOrientation;
        }
    }

    // class of camera work thread
    class CameraThread extends Thread {
        android.os.Handler innerHandler;
        Scanner parentScanner;

        // constructor
        CameraThread(Scanner scanner) {
            super();
            parentScanner = scanner;
        }

        // method for finishing thread
        public void finish() {
            handler.sendEmptyMessage(0);
            try {
                interrupt();
            } catch (Exception e) {
                //do nothing
            }
        }

        @Override
        public void run() {
            // prepare looper for camera thread
            android.os.Looper.prepare();

            // start camera
            //Scanner.this.camera = android.hardware.Camera.open();
            //camera.startPreview();

            // class for processing messages
            class ThreadHandler extends Handler {
                private final Camera val;

                // constructor
                ThreadHandler(Camera cam) {
                    super();
                    val = cam;
                }

                // handler for messages queue
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == QUIT) {
                        Looper.myLooper().quit();
                        Scanner.this.camera = null;
                        val.release();
                    }
                }
            }

            innerHandler = new ThreadHandler(camera);
            Scanner.this.cameraLatch.countDown();
            android.os.Looper.loop();
        }
    }
}