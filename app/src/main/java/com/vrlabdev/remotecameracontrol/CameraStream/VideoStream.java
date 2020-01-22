package com.vrlabdev.remotecameracontrol.CameraStream;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.CamcorderProfile;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StrictMode;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.codevog.android.license_library.MainInteractor;
import com.codevog.android.license_library.MainInteractorImpl;
import com.codevog.android.license_library.client_side_exception.BaseOcrException;
import com.vrlabdev.remotecameracontrol.MainActivity;


public class VideoStream {

    File file;
    public static final String LOG_TAG = "myLogs";
    public static Surface surface = null;

    public static CameraService[] myCameras = null;

    private CameraManager mCameraManager = null;
    public static final int CAMERA1 = 0;


    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler = null;

    private MediaCodec mCodec = null; // кодер
    private MediaRecorder mMediaRecorder;
    Surface mEncoderSurface; // Surface как вход данных для кодера5296


    private String mNextVideoAbsolutePath;

    ByteBuffer outPutByteBuffer;
    byte[] outDate = null;
    DatagramSocket udpSocket;


    //IP МЕНЯТЬ НА СТРОЧКЕ НИЖЕ
    String ip_address = "10.128.33.90"; //"192.168.31.238";  //IP МЕНЯТЬ ЗДЕСЬ      IP МЕНЯТЬ ЗДЕСЬ      IP МЕНЯТЬ ЗДЕСЬ      IP МЕНЯТЬ ЗДЕСЬ      IP МЕНЯТЬ ЗДЕСЬ      IP МЕНЯТЬ ЗДЕСЬ
    //IP МЕНЯТЬ НА СТРОЧКЕ ВЫШЕ



    InetAddress address;
    int port = 5005;

    private Context mContext;
    public static boolean flashlight = false;
    private TextureView texture;
    private CameraDevice cameraDevice;


    private boolean CameraInUse = false;
    public boolean recognition = false;

    private static final SparseIntArray ORIENTATONS = new SparseIntArray();
    static {
        ORIENTATONS.append(Surface.ROTATION_0, 90);
        ORIENTATONS.append(Surface.ROTATION_90, 0);
        ORIENTATONS.append(Surface.ROTATION_180, 270);
        ORIENTATONS.append(Surface.ROTATION_270, 180);
    }

    public boolean isRecordingVideo;


    public VideoStream(Context context) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();     //эти 2 строчки позволяют отправлять в сеть из основного потока
        StrictMode.setThreadPolicy(policy);
        mContext = context;
    }

    public void CameraBuild(int CameraMode) {
        if (CameraMode == 1 || CameraMode == 2) {
            openUDPsocket();
            setUpMediaCodec();
        }

        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            // Получение списка камер с устройства
            myCameras = new CameraService[mCameraManager.getCameraIdList().length];

            for (String cameraID : mCameraManager.getCameraIdList()) {
                Log.i(LOG_TAG, "cameraID: " + cameraID);
                int id = Integer.parseInt(cameraID);

                // создаем обработчик для камеры
                myCameras[id] = new CameraService(mCameraManager, cameraID, CameraMode);
            }
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    public void StartCamera() {
        setUpMediaCodec();
        if (myCameras[CAMERA1] != null) {
            if (!myCameras[CAMERA1].isOpen()) myCameras[CAMERA1].openCamera();
        }
    }

    public void StopStream()       //ОСТАНОВКА СТРИМА
    {
        if (myCameras[CAMERA1] != null) {
            try {
                myCameras[CAMERA1].StopCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void StopDrawing() {
        if (myCameras[CAMERA1] != null) {
            try {
                myCameras[CAMERA1].StopCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isStream() {
        if (myCameras[CAMERA1] != null) {
            if (myCameras[CAMERA1].CameraMode == 1 || myCameras[CAMERA1].CameraMode == 2) {
                return true;
            } else
                return false;
        }
        return false;
    }


    //===============================================================================================================
    // НАЧАЛО CameraService
    //===============================================================================================================

    public class CameraService {
        private String mCameraID;
        private CameraDevice mCameraDevice = null;
        private CameraCaptureSession mSession = null;
        private CaptureRequest.Builder mPreviewBuilder;
        private int CameraMode = -1;


        public CameraService(CameraManager cameraManager, String cameraID, int CameraMode) {

            mCameraManager = cameraManager;
            mCameraID = cameraID;
            this.CameraMode = CameraMode;
        }

        public void Toggle_light(boolean flashlight) {
            if (flashlight & mPreviewBuilder != null) {
                mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                try {
                    mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                } catch (Exception e) {
                }
            }

            if (!flashlight & mPreviewBuilder != null) {
                mPreviewBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                try {
                    mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                } catch (Exception e) {
                }
            }
        }       //ЛАМПОЧКА


        public void openCamera() {      //Пытаемся открыть и камеру и переходим в Callback
            try {
                mCameraManager.openCamera(mCameraID, mCameraCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                Log.i(LOG_TAG, e.getMessage());
            }
            CameraInUse = true;
        }

        public boolean isOpen() {       //Проверяем открыта ли камера
            if (mCameraDevice == null) {
                return false;
            } else {
                return true;
            }
        }


        private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {     //СЮДА ВОЗВРАЩАЕМСЯ ПОСЛЕ ОТКРЫТИЯ КАМЕРЫ

            @Override
            public void onOpened(CameraDevice camera) {
                mCameraDevice = camera;
                cameraDevice = mCameraDevice;
                Log.i(LOG_TAG, "Open camera  with id:" + mCameraDevice.getId());

                try {
                    setUpMediaRecorder();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                switch (CameraMode) {
                    case 0:
                        startDrawing();
                        break;
                    case 1:
                        startStream();
                        break;
                    case 2:
                        startDrawingAndStream();
                        break;
                }
            }

            @Override
            public void onDisconnected(CameraDevice camera) {
                mCameraDevice.close();

                Log.i(LOG_TAG, "disconnect camera  with id:" + mCameraDevice.getId());
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Log.i(LOG_TAG, "error! camera id:" + camera.getId() + " error:" + error);
            }
        };


        private void startDrawing() {
            SurfaceTexture surfacetexture = texture.getSurfaceTexture();
            surfacetexture.setDefaultBufferSize(720, 480);             //МИХАЛЫЧ ЭТО ЖЕ НАСТРОЙКА РАЗРЕШЕНИЯ КАМЕРЫ!!!!!!!!!!!1111111одиндинраз
            surface = new Surface(surfacetexture);

            try {
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                mPreviewBuilder.addTarget(surface);


                //отсюда
                mCameraDevice.createCaptureSession(Arrays.asList(surface),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mSession = session;
                                try {
                                    mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                            }
                        }, mBackgroundHandler);
                //доюда нужно все перенести в отедльную функцию с параметром List<Surface>
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }


        private void startStream() {
            try {
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                mPreviewBuilder.addTarget(mEncoderSurface);

                mCameraDevice.createCaptureSession(Arrays.asList(mEncoderSurface),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mSession = session;
                                try {
                                    mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                            }
                        }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }


        private void startDrawingAndStream() {
            SurfaceTexture surfacetexture = texture.getSurfaceTexture();
            surfacetexture.setDefaultBufferSize(3840, 2160);             //МИХАЛЫЧ ЭТО ЖЕ НАСТРОЙКА РАЗРЕШЕНИЯ КАМЕРЫ!!!!!!!!!!!1111111одиндинраз
            surface = new Surface(surfacetexture);

            try {
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                mPreviewBuilder.addTarget(surface);
                mPreviewBuilder.addTarget(mEncoderSurface);

                mCameraDevice.createCaptureSession(Arrays.asList(surface, mEncoderSurface),
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mSession = session;
                                try {
                                    mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {
                            }
                        }, mBackgroundHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }


        private int getCameraMode() {
            return CameraMode;
        }

        public void StopCamera() throws CameraAccessException
        {//
            if (mCameraDevice != null & mCodec != null) {

                mSession.stopRepeating();
                mSession.abortCaptures();

                mCodec.stop();
                mCodec.release();
                mEncoderSurface.release();
                surface.release();
                closeCamera();
                CameraInUse = false;
            }
        }

        public void closeCamera() {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }




        private String takePicture() throws CameraAccessException
        {
            if (myCameras[CAMERA1] == null) {
                return "";
            }
            CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());


            int width = 720;
            int height = 480;


            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());

            outputSurfaces.add(new Surface(texture.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);


            int rotation = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
            //captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATONS.get(rotation));


            Long tsLong = System.currentTimeMillis() / 1000;
            String ts = tsLong.toString();

            file = new File(Environment.getExternalStorageDirectory() + "/" + ts + ".jpg");

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;

                    image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);

                    try {
                        save(bytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (image != null) {
                            image.close();
                        }
                    }
                }
            };


            reader.setOnImageAvailableListener(readerListener,mBackgroundHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                   // Toast.makeText(mContext.getApplicationContext(),"Saved",Toast.LENGTH_SHORT).show();
                    try {

                        mCameraManager.openCamera(mCameraID, mCameraCallback, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.capture(captureBuilder.build(),captureListener,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgroundHandler);

            return "/sdcard/"+file.getName();
        }



        private void save (final byte[] bytes) throws IOException
        {
            OutputStream outputStream = null;

            outputStream = new FileOutputStream(file);
            outputStream.write(bytes);
            outputStream.close();

            if(recognition)
            {
                new Thread(new Runnable() {
                    public void run() {
                        while(bytes.length>file.length())
                        { }
                        final String [] massImages=new String[]{file.getAbsolutePath()};
                        OpenOpen openOpen = new OpenOpen();
                        MainInteractorImpl mainInteractor = new MainInteractorImpl((Activity) mContext);
                        mainInteractor.importLicense("5301015b883c9ba2525e4e4145f4d5d44810268f8c6defeea75920873aa974084352e433f578d8fcd61f454f19b723b73fd239fe1cb45742ce7738e3ff49e3edf5a8ab34a4c245fd63b3109230db1f95b51fb05a11c7025647dc97bd5afc129c665527646fe1a5b031a2be95f301520f2eec990e7d355a8f6ea8264d6779d68d35245f6d5b9bea0cdc8e69d9e1d4319bda422e2d53a815eb092ad4cea7cf08efcfc3b9fa1dae1cfcc33bd2ee043c1ccae74795d72aadb81b860b63c14c58d9710d7dce61e7c2970f9381983210cdc1f947230769b9953f520cbdf635b4ee7ed11a059ab6bb2acd9d9824b0832857fade588e8d42c2c959727754e015e2fd876b52b28410e1f675b6457a98fd4ebdb2534e0f06c8881226b24767570f0f0f18971a3e644d7673b9790d1ee495303f4a487e366dc829265131372f5a189484c29787ec32a90321585a4e257b192ad9af7234f34f5e2b973edb5e61900a556a56d63727d486e0913d7ade82087bdd04fc931d7b1ee5f2de4844a74e8dea2c65945e");
                        mainInteractor.doRecognize("b083c358-a424-4833-bfdc-3acf0c2db056", "052a13d0-9048-4f41-8c63-4a8130ee5b3c", massImages, openOpen);
                    }
                }).start();
            }
            int i=5;
            i=34;
        }


        class OpenOpen implements  MainInteractor.Callback{

            @Override
            public void recogOk(Map<String, String> map) {
                Collection<String> stringCollection=map.values();
                for(String str : stringCollection)
                {
                    Log.d("RECONITION    ", str);
                }
            }

            @Override
            public void recogError(BaseOcrException e) {
                Log.d("ERROR ERROR    ", e.getMessage());
            }

            private String getRez(String col) {
                String result = "result";
                if (col.length() < 200) {
// LogWrite(LOG_TYPE_COMMON, "EmptyResult__FileName: " + imageFile.getName());
                    return "Result is empty";
                } else {
                    col = col.substring(col.indexOf("\"result\""));
                    col = col.substring(10);
                    result = col.substring(0, col.indexOf("\""));

                    col = col.substring(col.indexOf("\"iso_size_type\""));
                    col = col.substring(17);

                    String subCol = col.substring(0, col.indexOf("\""));
                    if (subCol.length() == 4 || subCol.length() == 3) {
                        result = result + " : " + subCol;
                    } else {
                        result = result + " : Not recognize";
                    }
                    return result;
                }
            }
        }


        private void startRecordingVideo()
        {
            if (null == mCameraDevice)
            {
                return;
            }
            try
            {
                //setUpMediaRecorder();
                //mMediaRecorder.start();
                SurfaceTexture mTexture = texture.getSurfaceTexture();
                setUpMediaRecorder();
                mTexture.setDefaultBufferSize(720, 480);

                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                List<Surface> surfaces = new ArrayList<>();

                // Set up Surface for the camera preview
                Surface previewSurface = new Surface(mTexture);
                surfaces.add(previewSurface);
                mPreviewBuilder.addTarget(previewSurface);

                // Set up Surface for the MediaRecorder
                Surface recorderSurface = mMediaRecorder.getSurface();
                surfaces.add(recorderSurface);
                mPreviewBuilder.addTarget(recorderSurface);

                // Start a capture session
                // Once the session starts, we can update the UI and start recording
                mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        mSession = cameraCaptureSession;
                        try {
                            mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                        isRecordingVideo=true;

                        mMediaRecorder.start();
                    }


                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                        if (null != mContext) {
                            Toast.makeText(mContext, "Failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                }, mBackgroundHandler);

        } catch (CameraAccessException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        private void stopRecordingVideo() {
            // UI
isRecordingVideo=false;

            // Stop recording
            mMediaRecorder.stop();
            mMediaRecorder.reset();

            if (null != mContext) {
                Toast.makeText(mContext, "Video saved: " + mNextVideoAbsolutePath,
                        Toast.LENGTH_SHORT).show();
                Log.d("VIDEO    TAG     ", "Video saved: " + mNextVideoAbsolutePath);
            }

            mNextVideoAbsolutePath = null;
            startDrawing();
        }
    }


    //===============================================================================================================
    // КОНЕЦ CameraService
    //===============================================================================================================


    public void CameraStart(int cameraMode) {
        StopStream();
        CameraBuild(cameraMode);
        CameraControlChannel.getControl().stream.StartCamera();
    }

    public void CameraStart(int cameraMode, TextureView textureView) {
        StopStream();
        texture = textureView;
        CameraBuild(cameraMode);
        CameraControlChannel.getControl().stream.StartCamera();
    }


    public int getMode() {
        if (CameraInUse) {
            if (myCameras[CAMERA1] != null) {
                return myCameras[CAMERA1].getCameraMode();
            } else
                return -1;
        } else
            return -1;
    }

    public void SetTargetSurface(TextureView textureView) {
        texture = textureView;
    }


    private void setUpMediaCodec() {
        try {
            mCodec = MediaCodec.createEncoderByType("video/avc"); // H264 кодек

        } catch (Exception e) {
            Log.i(LOG_TAG, "нету кодека");
        }

        int width = 1920; // ширина видео
        int height = 1080; // высота видео
        int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface; // формат ввода цвета
        int videoBitrate = 15000000; // битрейт видео в bps (бит в секунду)
        int videoFramePerSecond = 30; // FPS
        int iframeInterval = 1; // I-Frame интервал в секундах

        MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, videoFramePerSecond);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iframeInterval);


        mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE); // конфигурируем кодек как кодер
        mEncoderSurface = mCodec.createInputSurface(); // получаем Surface кодера

        mCodec.setCallback(new EncoderCallback());
        mCodec.start(); // запускаем кодер
        Log.i(LOG_TAG, "запустили кодек");
    }


    private class EncoderCallback extends MediaCodec.Callback {

        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {

        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {

            outPutByteBuffer = mCodec.getOutputBuffer(index);
            outDate = new byte[info.size];
            outPutByteBuffer.get(outDate);


            int count = 0;
            int temp = outDate.length;

            do {//кромсаем на небольше килобайта
                byte[] ds;
                temp = temp - 1024;
                if (temp >= 0) {
                    ds = new byte[1024];
                } else {
                    ds = new byte[temp + 1024];
                }
                for (int i = 0; i < ds.length; i++) {
                    ds[i] = outDate[i + 1024 * count];
                }
                count = count + 1;
                try {
                    // Log.i(LOG_TAG, " outDate.length : " + ds.length);
                    DatagramPacket packet = new DatagramPacket(ds, ds.length, address, port);
                    udpSocket.send(packet);
                } catch (IOException e) {
                    Log.i(LOG_TAG, " не отправился UDP пакет   " + e);
                }
            }
            while (temp >= 0);

            mCodec.releaseOutputBuffer(index, false);
        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            Log.i(LOG_TAG, "Error: " + e);
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            Log.i(LOG_TAG, "encoder output format changed: " + format);
        }
    }





    public void startVideoRecord()
    {
        myCameras[CAMERA1].startRecordingVideo();
    }


    public void stopVideoRecord()
    {
        myCameras[CAMERA1].stopRecordingVideo();
    }
    /**
     * Setup media recorder
     */
    private void setUpMediaRecorder() throws IOException {


        if (null == mContext) {
            return;
        }
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath(mContext);
        }
        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);

        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
        /*mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(720, 480);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);*/

        mMediaRecorder.prepare();
    }
    private String getVideoFilePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + System.currentTimeMillis() + ".mp4";
    }


    private void openUDPsocket() {
        try {
            udpSocket = new DatagramSocket();

            Log.i(LOG_TAG, "  создали udp сокет");

        } catch (
                SocketException e) {
            Log.i(LOG_TAG, " не создали udp сокет");
        }

        try {
            address = InetAddress.getByName(ip_address);
            Log.i(LOG_TAG, "  есть адрес");
        } catch (Exception e) {
        }
    }


    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() throws InterruptedException
    {
        mBackgroundThread.quitSafely();

        mBackgroundThread.join();
        mBackgroundThread = null;
        mBackgroundHandler = null;
    }


    public String takePicture()
    {
        String picturePath = null;
        try {
            picturePath = myCameras[CAMERA1].takePicture();
            return picturePath;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return "";
    }
}
