package za.co.palota.honeywell;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.AudioManager;

import com.honeywell.aidc.AidcManager;
import com.honeywell.aidc.BarcodeFailureEvent;
import com.honeywell.aidc.BarcodeReadEvent;
import com.honeywell.aidc.BarcodeReader;
import com.honeywell.aidc.ScannerUnavailableException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HoneywellBarcodeScannerPlugin extends CordovaPlugin implements BarcodeReader.BarcodeListener {
    private static final String TAG = "HoneywellBarcodeScanner";

    private CallbackContext callbackContext;
    private static BarcodeReader barcodeReader;
    private AidcManager manager;
    private boolean isScanBlocked = false;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        Context context = cordova.getActivity().getApplicationContext();

        AidcManager.create(context, new AidcManager.CreatedCallback() {
            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                barcodeReader = manager.createBarcodeReader();
                if (barcodeReader != null) {

                    // register bar code event listener
                    barcodeReader.addBarcodeListener(HoneywellBarcodeScannerPlugin.this);

                    Map<String, Object> properties = new HashMap<String, Object>();
                    // Set Symbologies On/Off
                    properties.put(BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_GS1_128_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_CODE_39_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_CODE_39_BASE_32_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_UPC_A_ENABLE, true);
                    properties.put(BarcodeReader.PROPERTY_EAN_13_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_AZTEC_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_CODABAR_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_INTERLEAVED_25_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_PDF_417_ENABLED, true);
                    // Set Max Code 39 barcode length
                    properties.put(BarcodeReader.PROPERTY_CODE_39_MAXIMUM_LENGTH, 10);
                    // Turn on center decoding
                    properties.put(BarcodeReader.PROPERTY_CENTER_DECODE, true);
                    // Enable bad read response
                    properties.put(BarcodeReader.PROPERTY_NOTIFICATION_BAD_READ_ENABLED, false);
                    properties.put(BarcodeReader.PROPERTY_NOTIFICATION_GOOD_READ_ENABLED, false);
                    properties.put(BarcodeReader.PROPERTY_UPC_A_CHECK_DIGIT_TRANSMIT_ENABLED, true);
                    properties.put(BarcodeReader.PROPERTY_EAN_13_CHECK_DIGIT_TRANSMIT_ENABLED, true);
                    // Apply the settings
                    barcodeReader.setProperties(properties);

                    try {
                        barcodeReader.claim();
                    } catch (ScannerUnavailableException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

   @Override
public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (action.equals("onBarcodeScanned")) {
        this.callbackContext = callbackContext;
        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
        result.setKeepCallback(true);
        this.callbackContext.sendPluginResult(result);
    } else if (action.equals("setScanBlocked")) {
        boolean block = args.getBoolean(0); // Assume args contains a boolean
        isScanBlocked = block;
          if (block) {
            playBlockedScanSound(); // Riproduci il suono solo se block è true
        }
        callbackContext.success("Scan block set to: " + block);
        return true;
    } else if (action.equals("setGoodReadNotification")) {
        boolean enabled = args.getBoolean(0); // Assume il primo argomento è un boolean
        setGoodReadNotificationEnabled(enabled);
        callbackContext.success("Good read notification set to: " + enabled);
        return true;
    }
    return true;
}


    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        if (barcodeReader != null) {
            try {
                barcodeReader.claim();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                NotifyError("Scanner unavailable");
            }
        }
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        if (barcodeReader != null) {
            // release the scanner claim so we don't get any scanner
            // notifications while paused.
            barcodeReader.release();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (barcodeReader != null) {
            // unregister barcode event listener
            barcodeReader.removeBarcodeListener(this);
            barcodeReader.close();
            barcodeReader = null;
        }

        if (manager != null) {
            // close AidcManager to disconnect from the scanner service.
            // once closed, the object can no longer be used.
            manager.close();
        }
    }

    @Override
    public void onFailureEvent(BarcodeFailureEvent failureEvent) {
        NotifyError("Barcode failed");
    }

   @Override
public void onBarcodeEvent(final BarcodeReadEvent event) {
    if (isScanBlocked) {
        playBlockedScanSound();
        return; // Ignora l'evento
    }

    if (this.callbackContext != null) {
        try {
            JSONObject response = new JSONObject();

            response.put("data", event.getBarcodeData());
            response.put("code", event.getCodeId());
            response.put("charset", event.getCharset());
            response.put("aimId", event.getAimId());
            response.put("timestamp", event.getTimestamp());

            PluginResult result = new PluginResult(PluginResult.Status.OK, response);
            result.setKeepCallback(true);
            this.callbackContext.sendPluginResult(result);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}


    private void NotifyError(String error) {
        if (this.callbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.ERROR, error);
            result.setKeepCallback(true);
            this.callbackContext.sendPluginResult(result);
        }
    }

   private void playBlockedScanSound() {
    Context context = this.cordova.getActivity().getApplicationContext();

    // Crea il MediaPlayer per il suono specificato
    MediaPlayer mediaPlayer = MediaPlayer.create(context, context.getResources().getIdentifier("blocked_scan", "raw", context.getPackageName()));
    if (mediaPlayer != null) {
        // Ottieni l'AudioManager per controllare il volume
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        // Imposta il volume al massimo
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(mp -> mp.release()); // Rilascia il MediaPlayer dopo la riproduzione
        mediaPlayer.start();
    }
}

    private void setGoodReadNotificationEnabled(boolean enabled) {
        try {
            Map<String, Object> properties = new HashMap<>();
            properties.put(BarcodeReader.PROPERTY_NOTIFICATION_GOOD_READ_ENABLED, enabled);
            barcodeReader.setProperties(properties);
        } catch (Exception e) {
            e.printStackTrace();
            NotifyError("Failed to update good read notification: " + e.getMessage());
    }
}


}