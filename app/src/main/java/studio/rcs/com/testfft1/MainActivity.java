package studio.rcs.com.testfft1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import ca.uol.aig.fftpack.RealDoubleFFT;


public class MainActivity extends Activity implements OnClickListener {

    int frequency = 8000;/*44100;*/ /*this should be specified in Hz. As we know, the MediaRecorder samples audio at 8kHz,
    Cd quality audio is typically 44.1 kHz. Hz or hertz is the number of samples per second. Different Android handset hardware
    will be able to sample at differente sample rates. For this program, we'll use 11.025 Hz, which is another commonly used sample rate */

    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO; /*We need to specify the number of channels of audio to capture.
    The constant for this parameter are specified in the AudioFormat class and are self-explanatory
    AudioFormat.CHANNEL_CONFIGURATION_MONO
    AudioFormat.CHANNEL_CONFIGURATION_STEREO
    AudioFormat.CHANNEL_CONFIGURATION_INVALID
    AudioFormat.CHANNEL_CONFIGURATION_DEFAULT*/

    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT; /*Audio data format: PCM 16 bit per sample.
    The audio sample is a 16 bit signed integer typically stored as a Java short in a short array,
    but when the short is stored in a ByteBuffer, Sixteen bits will take up more space and processing power while the representation
    of the audio will be closer to reality.*/

    private RealDoubleFFT transformer;

    int blockSize = 256; /*transformer will be our FFT object, and we'll be dealing with 256 samples at a time from the AudioRecord
     object through the FFT object. The number of samples we use will correspond to the number of component frequencies
     we will get after we run them through the FFT object. We are free to choose a different size, but we do need concern
     ourselves with memory and performance issues as the math required to the calculation is processor-intensive. */

    Button startStopButton;
    boolean started = false;

    RecordAudio recordTask;  /*Função como AsynTask */

    ImageView imageView2;

    ImageView imageView; /*We'll be using an ImageView to display a Bitmap image. This image will represent the
     levels of the various frequencies that are in the current audio stream. To draw these levels, we'll use Canvas and Paint
     objects constructed from the Bitmap. */

    Bitmap bitmap;

    Canvas canvas;

    Canvas canvas2;

    Paint paint;

    Bitmap bitmapScale;


    //
    //
    //Provavelmente é para manipular o bitmap
    //MyImageView imageViewScale; /*Funçao como ImageView */
    int lado;
    int altura;
    int width;
    int height;
    //
    //
    //

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT>22){
            requestPermissions(new String[] {"android.permission.RECORD_AUDIO"}, 2);
        }


        DisplayMetrics display = this.getResources().getDisplayMetrics();
        width = display.widthPixels;
        height = display.heightPixels;

        startStopButton = findViewById(R.id.StartStopButton);
        startStopButton.setOnClickListener(this);

        transformer = new RealDoubleFFT(blockSize); /*transformer é funçao nativa da biblioteca importada fftPack*/

        imageView2 = findViewById(R.id.ImageView2);

        imageView = findViewById(R.id.ImageView);

        //bitmap = Bitmap.createBitmap(256,300,Bitmap.Config.ARGB_8888);
        //canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        //imageView.setImageBitmap(bitmap);
    }

    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        lado = imageView.getWidth();
        altura = imageView.getHeight();

        int lado2 = imageView2.getWidth();
        int altura2 = imageView2.getHeight();

        Log.e("Valor de lado e altura", Integer.toString(lado) + "+" + Integer.toString(altura));
        bitmap = Bitmap.createBitmap(lado,altura,Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        imageView.setImageBitmap(bitmap);

        //fazer a escala
        Paint paintScaleDisplay;
        paintScaleDisplay = new Paint();
        paintScaleDisplay.setColor(Color.WHITE);
        paintScaleDisplay.setStyle(Paint.Style.FILL);

        bitmapScale = Bitmap.createBitmap(lado2,altura2,Bitmap.Config.ARGB_8888);
        canvas2 = new Canvas(bitmapScale);
        canvas2.drawColor(Color.BLACK);

        canvas2.drawLine(0, altura2/2,  lado2, altura2/2, paintScaleDisplay);
        for(int i = 0,j = 0; i< lado2; i=i+(lado2/4), j++){
            for (int k = i; k<(i+(lado2/4)); k=k+(lado2/32)){
                canvas2.drawLine(k, altura2/2, k, (altura2/2-5), paintScaleDisplay);
            }
            canvas2.drawLine(i, (altura2/2+10), i, (altura2/2-5), paintScaleDisplay);
            String text = Integer.toString(j) + " KHz";
            canvas2.drawText(text, i, (altura2/2+10), paintScaleDisplay);
        }
        canvas2.drawBitmap(bitmapScale, 0, 0, paintScaleDisplay);

        //imageView2.setImageBitmap(bitmapScale);

    }

    @SuppressLint("StaticFieldLeak")
    private class RecordAudio extends AsyncTask<Void, double[], Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                int bufferSize = AudioRecord.getMinBufferSize(frequency,channelConfiguration,audioEncoding);

                AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        frequency,channelConfiguration,audioEncoding,bufferSize);

                short[] buffer = new short[blockSize];
                double[] toTransform = new double[blockSize];

                audioRecord.startRecording();
                while (started) {
                    int bufferReadResult = audioRecord.read(buffer, 0, blockSize);

                    for (int i=0; i<blockSize && i<bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0;
                    }
                    transformer.ft(toTransform);
                    publishProgress(toTransform);
                }
                audioRecord.stop();
            }catch (Throwable throwable) {
                Log.e("AudioRecord","Recording Failed");
            }
            return null;
        }

        protected void onProgressUpdate (double[]... toTransform) {
            canvas.drawColor(Color.BLACK);
            imageView2.setImageBitmap(bitmapScale);

            Log.e("Tamanho do tranform:",Integer.toString(toTransform[0].length));

            for (int i=0; i<toTransform[0].length;i++){
                int x=lado*i/256;
                int downy = (int) ((altura/2) - (toTransform[0][i]*10));
                int upy = altura/2;

                canvas.drawLine(x,downy,x,upy,paint);
            }
            imageView.invalidate();
        }
    }

    @SuppressLint("SetTextI18n")
    public void onClick(View view){
        if(started) {
            started = false;
            startStopButton.setText("Start");
            recordTask.cancel(true);
        }
        else {
            started = true;
            startStopButton.setText("Stop");
            recordTask = new RecordAudio();
            recordTask.execute();
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 2: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission Granted, Now you can access app without bug.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Permission Denied, App maybe get crashed.", Toast.LENGTH_LONG).show();
                }
                break;
            }

        }
    }
}
