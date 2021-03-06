package com.afa.devicesfiletransfer.services.transfer.sender;

import com.afa.devicesfiletransfer.domain.model.Device;
import com.afa.devicesfiletransfer.domain.model.TransferFile;

import java.util.List;

public interface FileSenderServiceLauncher {
    void send(List<Device> devices, List<TransferFile> files);
}
