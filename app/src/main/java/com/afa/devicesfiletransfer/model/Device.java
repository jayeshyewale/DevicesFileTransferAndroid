package com.afa.devicesfiletransfer.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.util.Objects;

public class Device implements Parcelable {
    private String name;
    private String os;
    private InetAddress address;

    public Device(String name, String os, InetAddress address) {
        this.name = name;
        this.os = os;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getOs() {
        return os;
    }

    public InetAddress getAddress() {
        return address;
    }

    public String getIpAddress() {
        return address.getHostAddress();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(os);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Device device = (Device) o;
        return address.equals(device.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    protected Device(Parcel in) {
        name = in.readString();
        os = in.readString();
    }

    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };
}
