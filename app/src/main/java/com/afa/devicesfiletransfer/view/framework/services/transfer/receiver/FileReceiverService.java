package com.afa.devicesfiletransfer.view.framework.services.transfer.receiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.afa.devicesfiletransfer.ConfigProperties;
import com.afa.devicesfiletransfer.R;
import com.afa.devicesfiletransfer.domain.model.Transfer;
import com.afa.devicesfiletransfer.framework.repository.TransfersRoomDatabaseRepository;
import com.afa.devicesfiletransfer.services.transfer.receiver.FileReceiverProtocol;
import com.afa.devicesfiletransfer.services.transfer.receiver.FilesReceiverListener;
import com.afa.devicesfiletransfer.usecases.SaveTransferUseCase;
import com.afa.devicesfiletransfer.util.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class FileReceiverService extends Service {
    private static final String CHANNEL_ID = FileReceiverService.class.getName() + "Channel";
    private SaveTransferUseCase saveTransferUseCase;
    private FilesReceiverListener filesReceiverListener;
    private ThreadPoolExecutor fileReceivingExecutor;
    private final IBinder binder = new FileReceiverService.LocalBinder();
    private List<FileReceiverProtocol.Callback> callbackReceivers = new ArrayList<>();
    private List<Transfer> inProgressTransfers = new ArrayList<>();

    public class LocalBinder extends Binder {
        FileReceiverService getService() {
            return FileReceiverService.this;
        }
    }

    public FileReceiverService() {
        fileReceivingExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
    }

    public void addCallbackReceiver(FileReceiverProtocol.Callback callbackReceiver) {
        if (!callbackReceivers.contains(callbackReceiver)) {
            callbackReceivers.add(callbackReceiver);
        }
    }

    public void removeCallbackReceiver(FileReceiverProtocol.Callback callbackReceiver) {
        callbackReceivers.remove(callbackReceiver);
    }

    public List<Transfer> getInProgressTransfers() {
        return Collections.unmodifiableList(inProgressTransfers);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        saveTransferUseCase = new SaveTransferUseCase(
                new TransfersRoomDatabaseRepository(getApplicationContext()));

        filesReceiverListener = new FilesReceiverListener(ConfigProperties.TRANSFER_SERVICE_PORT, new FilesReceiverListener.Callback() {
            @Override
            public void onTransferReceived(final InputStream inputStream) {
                final FileReceiverProtocol fileReceiver = FileReceiverService.this.createFileReceiver();
                fileReceivingExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        fileReceiver.receive(inputStream);
                    }
                });
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    filesReceiverListener.start();
                } catch (IOException e) {
                    stopSelf();
                }
            }
        }).start();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Receiver listener")
                .setContentText("Listening for incoming files")
                .setSmallIcon(R.drawable.icon_send)
                .build();

        startForeground(1, notification);

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_ID,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }

    }

    private FileReceiverProtocol createFileReceiver() {
        final FileReceiverProtocol fileReceiver = new FileReceiverProtocol(SystemUtils.getDownloadsDirectory());
        fileReceiver.setCallback(new FileReceiverProtocol.Callback() {
            @Override
            public void onInitializationFailure() {

            }

            @Override
            public  void onStart(Transfer transfer) {
                //TODO: Transfer received in notification
                inProgressTransfers.add(transfer);
                for (FileReceiverProtocol.Callback callbackReceiver : callbackReceivers) {
                    callbackReceiver.onStart(transfer);
                }
            }

            @Override
            public void onFailure(Transfer transfer, Exception e) {
                //TODO: Transfer error in notification
                inProgressTransfers.remove(transfer);
                for (FileReceiverProtocol.Callback callbackReceiver : callbackReceivers) {
                    callbackReceiver.onFailure(transfer, e);
                }
                persistTransfer(transfer);
            }

            @Override
            public void onProgressUpdated(Transfer transfer) {
                //TODO: Update progress in notification
                for (FileReceiverProtocol.Callback callbackReceiver : callbackReceivers) {
                    callbackReceiver.onProgressUpdated(transfer);
                }
            }

            @Override
            public void onSuccess(Transfer transfer, File file) {
                notifySystemAboutNewFile(file);
                inProgressTransfers.remove(transfer);
                for (FileReceiverProtocol.Callback callbackReceiver : callbackReceivers) {
                    callbackReceiver.onSuccess(transfer, file);
                }
                persistTransfer(transfer);
            }
        });

        return fileReceiver;
    }

    private void persistTransfer(final Transfer transfer) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                saveTransferUseCase.execute(transfer, new SaveTransferUseCase.Callback() {
                    @Override
                    public void onSuccess() {
                        Log.d("Transfers", "Received Transfer persisted");
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.d("Transfers", "Could not persist received transfer: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    private void notifySystemAboutNewFile(File file) {
        MediaScannerConnection.scanFile(FileReceiverService.this,
                new String[]{file.toString()},
                new String[]{file.getName()},
                null);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopSelf();
    }

    @Override
    public void onDestroy() {
        filesReceiverListener.stop();
        fileReceivingExecutor.shutdownNow();
        super.onDestroy();
    }
}
