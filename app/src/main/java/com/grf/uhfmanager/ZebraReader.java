package com.grf.uhfmanager;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.grf.utils.ProgressUtil;
import com.zebra.rfid.api3.ACCESS_OPERATION_CODE;
import com.zebra.rfid.api3.ACCESS_OPERATION_STATUS;
import com.zebra.rfid.api3.Antennas;
import com.zebra.rfid.api3.BEEPER_VOLUME;
import com.zebra.rfid.api3.ENUM_TRANSPORT;
import com.zebra.rfid.api3.ENUM_TRIGGER_MODE;
import com.zebra.rfid.api3.HANDHELD_TRIGGER_EVENT_TYPE;
import com.zebra.rfid.api3.InvalidUsageException;
import com.zebra.rfid.api3.OperationFailureException;
import com.zebra.rfid.api3.RFIDReader;
import com.zebra.rfid.api3.ReaderDevice;
import com.zebra.rfid.api3.Readers;
import com.zebra.rfid.api3.RfidEventsListener;
import com.zebra.rfid.api3.RfidReadEvents;
import com.zebra.rfid.api3.RfidStatusEvents;
import com.zebra.rfid.api3.START_TRIGGER_TYPE;
import com.zebra.rfid.api3.STATUS_EVENT_TYPE;
import com.zebra.rfid.api3.STOP_TRIGGER_TYPE;
import com.zebra.rfid.api3.TagData;
import com.zebra.rfid.api3.TriggerInfo;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ZebraReader {
    private static ZebraReader instance;
    private Context context;

    // ZebraReader
    private static Readers readers;
    private static ArrayList availableRFIDReaderList;
    private static ReaderDevice readerDevice;
    private static RFIDReader reader;
    private static String TAG = "DEMO";
    // TextView textView, TagId;
    private EventHandler eventHandler;

    private ZebraReader(Context context) {
        this.context = context.getApplicationContext(); // avoid memory leaks
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new ZebraReader(context);
        }
    }

    public static Context getOtherContext() {
        return otherContext;
    }

    public static void setOtherContext(Context otherContext) {
        ZebraReader.otherContext = otherContext;
    }

    public static Context otherContext;

    public static ZebraReader getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ZebraReader is not initialized. Call ZebraReader.init(context) first.");
        }
        return instance;
    }

    public void connectReader() {
        Toast.makeText(context, "Connecting...", Toast.LENGTH_SHORT).show();
        // reader connect logic here
    }

    public boolean isConnected() {
        return reader != null && reader.isConnected();
    }

    public void InitReader(ZebraReaderConnectionCallback callback) {
        try {
            Log.e(TAG, "InitReader: here ");
            if (readers == null) {
                Log.e(TAG, "InitReader: here2 ");
                readers = new Readers(this.context, ENUM_TRANSPORT.ALL);

            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler mainHandler = new Handler(Looper.getMainLooper());

            executor.execute(() -> {
                boolean isConnected = false;

                try {
                    Thread.sleep(0); // Add delay if needed

                    if (readers != null && readers.GetAvailableRFIDReaderList() != null) {
                        availableRFIDReaderList = readers.GetAvailableRFIDReaderList();
                        if (!availableRFIDReaderList.isEmpty()) {
                            for (Object device : availableRFIDReaderList) {
                                System.out.println("deviceid " + device);
                            }
                            readerDevice = (ReaderDevice) availableRFIDReaderList.get(0);
                            reader = readerDevice.getRFIDReader();

                            if (!reader.isConnected()) {
                                reader.connect();
                                ConfigureReader(1);
                                isConnected = true;
                            }
                        }
                    }

                } catch (InterruptedException | InvalidUsageException |
                         OperationFailureException e) {
                    e.printStackTrace();
                }

                boolean finalIsConnected = isConnected;
                mainHandler.post(() -> {
                    ProgressUtil.dismiss();
                    if (finalIsConnected) {
                        //Toast.makeText(context, "Reader Connected", Toast.LENGTH_LONG).show();
                        //  textView.setText("Reader connected");
                    }
                    // Send result back via callback
                    callback.onResult(finalIsConnected);
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
            callback.onResult(false); // Handle failure
        }
    }

    public boolean ConfigureReader(int type) {
        try {
            if (!reader.isConnected()) return false;
            reader.Config.setBeeperVolume(BEEPER_VOLUME.HIGH_BEEP);
            TriggerInfo triggerInfo = new TriggerInfo();
            triggerInfo.StartTrigger.setTriggerType(START_TRIGGER_TYPE.START_TRIGGER_TYPE_IMMEDIATE);
            triggerInfo.StopTrigger.setTriggerType(STOP_TRIGGER_TYPE.STOP_TRIGGER_TYPE_IMMEDIATE);

            if (eventHandler == null) eventHandler = new EventHandler();
            reader.Events.addEventsListener(eventHandler);

            Antennas.AntennaRfConfig config = reader.Config.Antennas.getAntennaRfConfig(1);
            config.setTransmitPowerIndex(297);
            config.setrfModeTableIndex(0);
            config.setTari(0);
            reader.Config.Antennas.setAntennaRfConfig(1, config);

            reader.Events.setHandheldEvent(true);
            reader.Events.setTagReadEvent(true);
            reader.Events.setAttachTagDataWithReadEvent(false);

            reader.Config.setTriggerMode(type == 1 ? ENUM_TRIGGER_MODE.RFID_MODE : ENUM_TRIGGER_MODE.BARCODE_MODE, true);
            reader.Config.setStartTrigger(triggerInfo.StartTrigger);
            reader.Config.setStopTrigger(triggerInfo.StopTrigger);


            return true;
        } catch (InvalidUsageException e) {
            e.printStackTrace();
            return false;
        } catch (OperationFailureException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public void disconnectReader() {
        try {
            if (reader != null) {
                reader.Events.removeEventsListener(eventHandler);
                reader.disconnect();
                Toast.makeText(this.context, "Disconnecting reader", Toast.LENGTH_LONG).show();
                reader = null;
                readers.Dispose();
                readers = null;
            }
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void StartInventory() {
        try {
            Log.e("", "hit times");
            reader.Actions.Inventory.perform();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    public void StopInventory() {
        try {
            reader.Actions.Inventory.stop();
        } catch (InvalidUsageException e) {
            e.printStackTrace();
        } catch (OperationFailureException e) {
            e.printStackTrace();
        }
    }

    private ZebraTagReadListener epcReadListener;

    public void setOnEpcReadListener(ZebraTagReadListener listener) {
        try {
            this.epcReadListener = listener;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Read/Status Notify handler
    // Implement the RfidEventsLister class to receive event notifications
    public class EventHandler implements RfidEventsListener {
        // Read Event Notification
        public void eventReadNotify(RfidReadEvents e) {
            // Recommended to use new method getReadTagsEx for better performance in case of large tag population
            TagData[] myTags = reader.Actions.getReadTags(100);

            if (myTags != null) {
                for (int index = 0; index < myTags.length; index++) {
                    // Log.d(TAG, "Tag ID " + myTags[index].getTagID());
                    String epcStr = myTags[index].getTagID();
                    int rssi = myTags[index].getPeakRSSI();

                    Log.d(TAG, "Tag ID " + myTags[index].getTagID() + rssi);
                    if (!epcStr.isEmpty() && epcStr.length() > 8) {

                        if (epcReadListener != null) {
                            epcReadListener.onTagRead(epcStr, rssi);
                        }
//                        if (MainActivity.isCheckStockPage) {
//                            MainActivity.checkStockFragment.executeTask(epcStr);
//                        } else if (MainActivity.ctx.IdentifyItemVisible) {
//                            IdentifyItem.identifyItemOnline.BindEpcAndCheck(epcStr);
//                        } else {
//                            InventoryItemActivity.inventoryItemActivity.addDataToList(epcStr);
//                        }


                    }

                   /* if (myTags[index].getOpCode() == ACCESS_OPERATION_CODE.ACCESS_OPERATION_READ &&
                            myTags[index].getOpStatus() == ACCESS_OPERATION_STATUS.ACCESS_SUCCESS) {
                        if (myTags[index].getMemoryBankData().length() > 0) {
                            Log.d(TAG, " Mem Bank Data " + myTags[index].getMemoryBankData());
                        }
                    }*/
                }
            }
        }

        // Status Event Notification
        private final ExecutorService executorEvent = Executors.newSingleThreadExecutor();

        // Status Event Notification
        public void eventStatusNotify(RfidStatusEvents rfidStatusEvents) {
            Log.d(TAG, "Status Notification: " + rfidStatusEvents.StatusEventData.getStatusEventType());

            if (rfidStatusEvents.StatusEventData.getStatusEventType() == STATUS_EVENT_TYPE.HANDHELD_TRIGGER_EVENT) {
                HANDHELD_TRIGGER_EVENT_TYPE triggerEvent =
                        rfidStatusEvents.StatusEventData.HandheldTriggerEventData.getHandheldEvent();

                if (triggerEvent == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_PRESSED) {
                    executorEvent.execute(() -> {
                        //  InventoryItemActivity.inventoryItemActivity.ZebraTriggerPress(1);
                        // Log.e(TAG, "eventStatusNotify: HANDHELD TRIGGER PRESSED");
                       /* try {
                            reader.Actions.Inventory.perform();

                        } catch (InvalidUsageException | OperationFailureException e) {
                            e.printStackTrace();
                        }*/
                    });
                }

                if (triggerEvent == HANDHELD_TRIGGER_EVENT_TYPE.HANDHELD_TRIGGER_RELEASED) {
                    //  Log.e(TAG, "eventStatusNotify: HANDHELD TRIGGER RELEASED");
                    executorEvent.execute(() -> {
                        //  InventoryItemActivity.inventoryItemActivity.ZebraTriggerPress(2);
                       /* try {
                            reader.Actions.Inventory.stop();
                        } catch (InvalidUsageException | OperationFailureException e) {
                            e.printStackTrace();
                        }*/
                    });
                }
            }
        }
    }
}
