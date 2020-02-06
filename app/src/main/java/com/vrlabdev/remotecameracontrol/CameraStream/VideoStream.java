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
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.codevog.android.license_library.MainInteractor;
import com.codevog.android.license_library.MainInteractorImpl;
import com.codevog.android.license_library.client_side_exception.BaseOcrException;
import com.vrlabdev.remotecameracontrol.File_Post;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;


public class VideoStream {

    private File file;
    private static final String LOG_TAG = "myLogs";



    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler = null;

    private CameraManager mCameraManager = null;
    private static CameraService myCamera = null;
    private static Surface surface = null;
    private MediaCodec mCodec = null; // кодер
    private MediaRecorder mMediaRecorder;
    private Surface mEncoderSurface; // Surface как вход данных для кодера5296


    private String videoPath;


    private Context mContext;
    private Activity mActivity;
    private TextureView texture;
    private CameraDevice cameraDevice;

    private boolean isRecordingVideo=false;


    public VideoStream(Context context,Activity activity) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();     //эти 2 строчки позволяют отправлять в сеть из основного потока
        StrictMode.setThreadPolicy(policy);
        mContext = context;
        mActivity = activity;
    }

    public void CameraBuild() {
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try
        {
            assert mCameraManager != null;
            String [] CamerasList = mCameraManager.getCameraIdList();


            // создаем обработчик для камеры
            myCamera = new CameraService(mCameraManager, CamerasList[0]);
            StartCamera();
        }
        catch (CameraAccessException e) {
            Log.e(LOG_TAG, ""+e.getMessage());
            e.printStackTrace();
        }
    }

    private void StartCamera() {
        if (myCamera != null) {
            if (!myCamera.isOpen())
            {
                myCamera.openCamera();
            }
        }
    }


    public void StopDrawing() {
        if (myCamera != null) {
            try {
                myCamera.StopCamera();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }
    public void StartDrawing() {
        if (myCamera != null) {
                myCamera.startDrawing();
        }
    }


    //===============================================================================================================
    // НАЧАЛО CameraService
    //===============================================================================================================

    public class CameraService {
        private String mCameraID;
        private CameraDevice mCameraDevice = null;
        private CameraCaptureSession mSession = null;
        private CaptureRequest.Builder mPreviewBuilder;

        CameraService(CameraManager cameraManager, String cameraID) {

            mCameraManager = cameraManager;
            mCameraID = cameraID;
        }

        void openCamera() {      //Пытаемся открыть и камеру и переходим в Callback
            try {
                mCameraManager.openCamera(mCameraID, mCameraCallback, mBackgroundHandler);
            } catch (CameraAccessException e) {
                Log.i(LOG_TAG, ""+e.getMessage());
            }
        }

        boolean isOpen() {       //Проверяем открыта ли камера
            return (mCameraDevice == null);
        }

        private CameraDevice.StateCallback mCameraCallback = new CameraDevice.StateCallback() {     //СЮДА ВОЗВРАЩАЕМСЯ ПОСЛЕ ОТКРЫТИЯ КАМЕРЫ
            @Override
            public void onOpened(@NotNull CameraDevice camera) {
                mCameraDevice = camera;
                cameraDevice = mCameraDevice;
                Log.i(LOG_TAG, "Open camera  with id:" + mCameraDevice.getId());

                //
                //раскомментировать это, если нужно постоянное изображение с камеры
                //
                startDrawing();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                mCameraDevice.close();

                Log.i(LOG_TAG, "disconnect camera  with id:" + mCameraDevice.getId());
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice camera, int error) {
                Log.i(LOG_TAG, "error! camera id:" + camera.getId() + " error:" + error);
            }
        };

        /**
         *  простой вывод изображения с камеры. Preview
         */
        void startDrawing() {
            setUpMediaCodec();
            SurfaceTexture surfacetexture = texture.getSurfaceTexture();
            surfacetexture.setDefaultBufferSize(320, 240);             //НАСТРОЙКА РАЗРЕШЕНИЯ КАМЕРЫ
            surface = new Surface(surfacetexture);

            try {
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                mPreviewBuilder.addTarget(surface);

                mCameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(@NotNull CameraCaptureSession session) {
                                mSession = session;
                                try {
                                    Thread.sleep(200);
                                    boolean BB=CameraControlChannel.getControl().isBusy;
                                    if(!BB)
                                        mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);

                                } catch (InterruptedException | CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(@NotNull CameraCaptureSession session) {
                            }
                        }, mBackgroundHandler);
            }
            catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }


         void StopCamera() throws CameraAccessException
        {
            if (mCameraDevice != null & mCodec != null) {

                mSession.stopRepeating();
                mSession.abortCaptures();

                mCodec.stop();
                mCodec.release();
                mEncoderSurface.release();
                surface.release();
                //closeCamera();
            }
        }

        public void closeCamera() {
            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }


        private void takePicture(final boolean recognition) throws CameraAccessException {
            if (myCamera == null)
            {
                return;
            }

            int width = 4608;
            int height = 3456;
            /*CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);

            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            Size[] jpegSizes = null;

            jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

Log.d("QQQ",String.valueOf(jpegSizes.length));*/

            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);

            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());

            outputSurfaces.add(new Surface(texture.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);

            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_HIGH_QUALITY);
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF);
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT);


            Long tsLong = System.currentTimeMillis() / 1000;
            String ts = tsLong.toString();

            file = new File(Environment.getExternalStorageDirectory() + "/" + ts + ".jpg");

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = reader.acquireLatestImage();
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.capacity()];
                    buffer.get(bytes);

                    try {
                        save(bytes,recognition);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        image.close();
                    }
                }
            };
            reader.setOnImageAvailableListener(readerListener,null);


            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NotNull CameraCaptureSession session,@NotNull  CaptureRequest request,@NotNull  TotalCaptureResult result) {
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
                public void onConfigured(@NotNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.capture(captureBuilder.build(),captureListener,mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NotNull CameraCaptureSession cameraCaptureSession) {

                }
            },mBackgroundHandler);
        }



        private void save (final byte[] bytes,boolean recognition) throws IOException
        {
            OutputStream outputStream;
            Log.d("QQQ",file.getName());
            outputStream = new FileOutputStream(file.getAbsoluteFile());
            outputStream.write(bytes);
            outputStream.close();

            //File storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            //File imageFile = File.createTempFile(file.getName(), ".jpg", storageDir);

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(!recognition)
            {
                CameraControlChannel.getControl().isBusy=false;
                File_Post filePost = new File_Post();
                filePost.TransieveFile(file);
                return;        //если просто сделать фото, то закончить функцию здесь. Если с распознавнаием, то выполнять дальше
            }


            final String [] massImages=new String[]{file.getAbsolutePath()};
            OpenOpen openOpen = new OpenOpen();
            MainInteractorImpl mainInteractor = new MainInteractorImpl(mActivity);
            Log.d("FILE_FILE", file.getAbsolutePath()+"   "+file.length());

            //File downloadDirectory = new File("/sdcard/Android/data/com.vrlabdev.remotecameracontrol/files/Download/");
            //downloadDirectory.mkdir();

            //mainInteractor.importServerKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCTO3et7a3NlDcPPbtJSBxI9MH6Dk6PE6zptZwp+6L3ijh7PxR0uyNaSSWnmQmzYxZNsGyBlGs+dQhlc4HFUjCaOVBaSDNaFaqXdfEm2TluLg5IhjxZLSbhYvLcgh4WEBernnWhjrRSXzV3AWRfiGBQqFleV09Xrp+vuQxn3BhoawIDAQAB");
            //String licenseString = mainInteractor.generateLicenseRequest("b083c358-a424-4833-bfdc-3acf0c2db056", "052a13d0-9048-4f41-8c63-4a8130ee5b3c");
            // writeToFile(new File("/sdcard/licenseRequest"),licenseString);
            //Log.d("TWERQ",licenseString);

            mainInteractor.importLicense("863f8b0c4824a9d8903d0f71b92959e1b55cef8d926b6ca4463b8be1f9a363b1352f94cfbf9b1a9505887c277a78c24e4a6a52e1301c8405624c8629871a2ee474015451500f8f22d9880c2dd304de447712360d97207bdd7392f3e63f843df48fa76b25e2e6acd982c2fbf5f99ddc7d16d83ed395d61415bc7652b910f027e844dff7f42a6c925a06094ec1cda85fb24360160fa3bb4dd444708a269d26e7737891c28eb96f6003a891eeeeac83bf767c8ae22efcd6224a823f2ea6b61862a38d4d996ed9424dcd5e52d818d2eaa0a6ac86435175c9b82d5d58579ccfa41412f768577eae23b58d64aad8898f52cc229683b05a05a0fb2df169a4fff978e99f707f56d155e52d75e3d72926939218ca07aa0f6bc15c98be9e83c91cc3d86fd0d29c984af6c1bbbe59e692746c339e636ac177d09e02bd2ff8536a3b84327ff46694a3653dd6c83263195fc6640cbf86d58a5b090060c98d7c255a4fb92c4f6274ca889241c44ad2b70d5b11b7bab12c5d175cf85e1671cf2ec77f4fcbfc07cb");
            mainInteractor.doRecognize("b083c358-a424-4833-bfdc-3acf0c2db056", "052a13d0-9048-4f41-8c63-4a8130ee5b3c", massImages, openOpen);
        }


        class OpenOpen implements  MainInteractor.Callback{

            @Override
            public void recogOk(Map<String, String> map) {
                Collection<String> str = map.values();
                for (String col : str) {
                    String result = getRez(col);
                    Log.d("Result",result);
                    //поделить результат распознавания наномер контейнера и исо-код
                    String [] ResultArray = {"Empty","Empty"};
                    if(result.contains(":")) {
                        ResultArray = result.split(" : ");
                    }
                    JSONObject jsonObject = new JSONObject();
                    try {
                        jsonObject.put("ContainerNumber",ResultArray[0]);
                        jsonObject.put("ISOcode",ResultArray[1]);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    CameraControlChannel.getControl().jsonImageData=jsonObject;

                    CameraControlChannel.getControl().isBusy=false;
                    File_Post filePost = new File_Post();
                    filePost.TransieveFile(file);
                }
            }

            @Override
            public void recogError(BaseOcrException e) {
                Log.e("ERROR", e.getMessage());

                String [] ResultArray = {"Empty","Empty"};
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("ContainerNumber",ResultArray[0]);
                    jsonObject.put("ISOcode",ResultArray[1]);
                } catch (JSONException except) {
                    except.printStackTrace();
                }
                CameraControlChannel.getControl().jsonImageData=jsonObject;

                CameraControlChannel.getControl().isBusy=false;

                File_Post filePost = new File_Post();
                filePost.TransieveFile(file);
            }

            private String getRez(String col) {
                String result;
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


        private void startRecordingVideo() throws IOException {
            if (null == mCameraDevice)
            {
                return;
            }
            try
            {
                setUpMediaRecorder();
                SurfaceTexture mTexture = texture.getSurfaceTexture();

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

        }
            catch (CameraAccessException e)
            {
                e.printStackTrace();
            }
        }



        private void stopRecordingVideo() {
            isRecordingVideo=false;

            // Stop recording
            mMediaRecorder.stop();
            mMediaRecorder.reset();

            if (null != mContext) {
                Toast.makeText(mContext, "Video saved: " + videoPath,
                        Toast.LENGTH_SHORT).show();
                Log.d("VIDEO    TAG     ", "Video saved: " + videoPath);
            }

            videoPath = null;
            //startDrawing();
        }
    }


    //===============================================================================================================
    // КОНЕЦ CameraService
    //===============================================================================================================


    public void SetTargetSurface(TextureView textureView) {
        texture = textureView;
    }

    private void setUpMediaCodec() {
        try {
            mCodec = MediaCodec.createEncoderByType("video/avc"); // H264 кодек
        } catch (Exception e) {
            Log.i(LOG_TAG, "нету кодека");
        }

        int width = 720; // ширина видео
        int height = 480; // высота видео
        int colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface; // формат ввода цвета
        int videoBitrate = 10000000; // битрейт видео в bps (бит в секунду)
        int videoFramePerSecond = 15; // FPS
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
        public void onInputBufferAvailable(@NotNull MediaCodec codec, int index) {
        }

        @Override
        public void onOutputBufferAvailable(@NotNull MediaCodec codec, int index, @NotNull MediaCodec.BufferInfo info) {
        }

        @Override
        public void onError(@NotNull MediaCodec codec, @NotNull MediaCodec.CodecException e) {
            Log.i(LOG_TAG, "Error: " + e);
        }

        @Override
        public void onOutputFormatChanged(@NotNull MediaCodec codec, @NotNull MediaFormat format) {
            Log.i(LOG_TAG, "encoder output format changed: " + format);
        }
    }


    public void startVideoRecord() throws IOException {
        myCamera.startRecordingVideo();
    }


    public void stopVideoRecord()
    {
        myCamera.stopRecordingVideo();
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
        if (videoPath == null || videoPath.isEmpty()) {
            videoPath = getVideoFilePath(mContext);
        }
        mMediaRecorder.setOutputFile(videoPath);

        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
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
        return (dir == null ? "" : (dir.getAbsolutePath() + "/")) + System.currentTimeMillis() + ".mp4";
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


    public void takePicture(boolean recognition)
    {
        try {
             myCamera.takePicture(recognition);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public static void writeToFile(File file, String text) {
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(text);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
