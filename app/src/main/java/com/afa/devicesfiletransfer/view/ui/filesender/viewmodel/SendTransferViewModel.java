package com.afa.devicesfiletransfer.view.ui.filesender.viewmodel;

import com.afa.devicesfiletransfer.domain.model.Device;
import com.afa.devicesfiletransfer.domain.model.Pair;
import com.afa.devicesfiletransfer.domain.model.Transfer;
import com.afa.devicesfiletransfer.domain.model.TransferFile;
import com.afa.devicesfiletransfer.services.transfer.sender.FileSenderProtocol;
import com.afa.devicesfiletransfer.services.transfer.sender.FileSenderServiceInteractor;
import com.afa.devicesfiletransfer.services.transfer.sender.FileSenderServiceLauncher;
import com.afa.devicesfiletransfer.view.framework.livedata.LiveEvent;
import com.afa.devicesfiletransfer.view.model.AlertModel;
import com.afa.devicesfiletransfer.view.model.ErrorModel;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SendTransferViewModel extends ViewModel {
    private final List<Transfer> transfers;
    private final MutableLiveData<List<Transfer>> transfersLiveData;
    private final MutableLiveData<List<TransferFile>> attachedFilesLiveData;
    private final LiveEvent<Transfer> onTransferProgressUpdatedEvent;
    private final LiveEvent<Pair<Transfer, TransferFile>> onTransferSucceededEvent;
    private final LiveEvent<Pair<Transfer, ErrorModel>> onSendTransferErrorEvent;
    private final LiveEvent<AlertModel> alertEvent;
    private final LiveEvent<ErrorModel> errorEvent;
    private List<TransferFile> attachedFiles;
    private FileSenderServiceLauncher fileSenderExecutor;
    private FileSenderServiceInteractor fileSenderServiceInteractor;

    public SendTransferViewModel(FileSenderServiceLauncher fileSenderExecutor,
                                 FileSenderServiceInteractor fileSenderServiceInteractor) {
        transfers = new ArrayList<>();
        transfersLiveData = new MutableLiveData<>();
        onTransferProgressUpdatedEvent = new LiveEvent<>();
        onTransferSucceededEvent = new LiveEvent<>();
        attachedFiles = new ArrayList<>();
        attachedFilesLiveData = new MutableLiveData<>();
        onSendTransferErrorEvent = new LiveEvent<>();
        alertEvent = new LiveEvent<>();
        errorEvent = new LiveEvent<>();

        this.fileSenderExecutor = fileSenderExecutor;
        this.fileSenderServiceInteractor = fileSenderServiceInteractor;
        this.fileSenderServiceInteractor.setCallback(new FileSenderProtocol.Callback() {

            @Override
            public void onInitializationFailure(FileSenderProtocol fileSenderProtocol) {
                
            }

            @Override
            public void onTransferInitializationFailure(Transfer transfer, Exception e) {
                triggerErrorEvent("Sending error", e.getMessage());
            }

            @Override
            public void onStart(Transfer transfer) {
                transfers.add(transfer);
                transfersLiveData.postValue(transfers);
            }

            @Override
            public void onFailure(Transfer transfer, Exception e) {
                triggerErrorEvent("Sending error", e.getMessage());
            }

            @Override
            public void onProgressUpdated(Transfer transfer) {
                onTransferProgressUpdatedEvent.postValue(transfer);
            }

            @Override
            public void onSuccess(Transfer transfer, TransferFile file) {
                onTransferSucceededEvent.postValue(new Pair<>(transfer, file));
            }
        });
    }

    public void onShowView() {
        fileSenderServiceInteractor.receive();
    }

    public MutableLiveData<List<Transfer>> getTransfersLiveData() {
        return transfersLiveData;
    }

    public MutableLiveData<Transfer> getOnTransferProgressUpdatedEvent() {
        return onTransferProgressUpdatedEvent;
    }

    public MutableLiveData<Pair<Transfer, TransferFile>> getOnTransferSucceededEvent() {
        return onTransferSucceededEvent;
    }

    public MutableLiveData<List<TransferFile>> getAttachedFiles() {
        return attachedFilesLiveData;
    }

    public LiveEvent<AlertModel> getAlertEvent() {
        return alertEvent;
    }

    public LiveEvent<ErrorModel> getErrorEvent() {
        return errorEvent;
    }

    public void attachFiles(List<TransferFile> files) {
        this.attachedFiles = files;
        this.attachedFilesLiveData.postValue(files);
    }

    public void sendFiles(List<Device> devices) {
        if (attachedFiles.isEmpty()) {
            triggerErrorEvent("No file attached", "You must attach a file");
            return;
        }

        if (devices == null || devices.isEmpty()) {
            showAlert("No device selected",
                    "You must select one or more devices to send the file");
            return;
        }

        fileSenderExecutor.send(devices, attachedFiles);
    }

    private void triggerSendTransferErrorEvent(Transfer transfer, ErrorModel error) {
        onSendTransferErrorEvent.postValue(new Pair<>(transfer, error));
    }

    private void triggerErrorEvent(String title, String message) {
        errorEvent.postValue(new ErrorModel(title, message));
    }

    private void showAlert(String title, String message) {
        alertEvent.postValue(new AlertModel(title, message));
    }

    public void onHideView() {
        if (fileSenderServiceInteractor != null)
            fileSenderServiceInteractor.stop();
    }

    @Override
    protected void onCleared() {
        fileSenderServiceInteractor.setServiceConnectionCallback(null);
        fileSenderServiceInteractor.setCallback(null);
        super.onCleared();
    }
}
