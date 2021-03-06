package com.afa.devicesfiletransfer.view.framework.services.transfer.receiver;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.afa.devicesfiletransfer.domain.model.Transfer;
import com.afa.devicesfiletransfer.services.ServiceConnectionCallback;
import com.afa.devicesfiletransfer.services.transfer.receiver.FileReceiverProtocol;
import com.afa.devicesfiletransfer.services.transfer.receiver.FileReceiverServiceInteractor;

import java.util.List;

public class FileReceiverServiceInteractorImpl implements FileReceiverServiceInteractor {

    private final Context context;
    private boolean mBound = false;
    private ServiceConnectionCallback serviceConnectionCallback;
    private FileReceiverProtocol.Callback callback;
    private FileReceiverService boundService;

    public FileReceiverServiceInteractorImpl(Context context) {
        this.context = context;
    }

    @Override
    public void setServiceConnectionCallback(ServiceConnectionCallback callback) {
        this.serviceConnectionCallback = callback;
    }

    @Override
    public void setCallback(FileReceiverProtocol.Callback callback) {
        this.callback = callback;
    }

    @Override
    public List<Transfer> getInProgressTransfers() {
        if (!mBound) {
            throw new IllegalStateException("The service has not been started");
        }

        return boundService.getInProgressTransfers();
    }

    @Override
    public void receive() {
        Intent serviceIntent = new Intent(context, FileReceiverService.class);
        context.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void stop() {
        if (boundService != null)
            boundService.removeCallbackReceiver(callback);
        if (mBound)
            context.unbindService(connection);
    }

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            FileReceiverService.LocalBinder binder = (FileReceiverService.LocalBinder) service;
            boundService = binder.getService();
            boundService.addCallbackReceiver(callback);
            mBound = true;

            if (serviceConnectionCallback != null) {
                serviceConnectionCallback.onConnect();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;

            if (serviceConnectionCallback != null) {
                serviceConnectionCallback.onDisconnect();
            }
        }
    };
}
