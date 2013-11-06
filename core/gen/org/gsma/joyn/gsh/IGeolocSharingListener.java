/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\gsh\\IGeolocSharingListener.aidl
 */
package org.gsma.joyn.gsh;
/**
 * Callback methods for geoloc sharing events
 */
public interface IGeolocSharingListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.gsh.IGeolocSharingListener
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.gsh.IGeolocSharingListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.gsh.IGeolocSharingListener interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.gsh.IGeolocSharingListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.gsh.IGeolocSharingListener))) {
return ((org.gsma.joyn.gsh.IGeolocSharingListener)iin);
}
return new org.gsma.joyn.gsh.IGeolocSharingListener.Stub.Proxy(obj);
}
@Override public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_onSharingStarted:
{
data.enforceInterface(DESCRIPTOR);
this.onSharingStarted();
reply.writeNoException();
return true;
}
case TRANSACTION_onSharingAborted:
{
data.enforceInterface(DESCRIPTOR);
this.onSharingAborted();
reply.writeNoException();
return true;
}
case TRANSACTION_onSharingError:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.onSharingError(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onSharingProgress:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
long _arg1;
_arg1 = data.readLong();
this.onSharingProgress(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_onGeolocShared:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.chat.Geoloc _arg0;
if ((0!=data.readInt())) {
_arg0 = org.gsma.joyn.chat.Geoloc.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.onGeolocShared(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.gsma.joyn.gsh.IGeolocSharingListener
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
@Override public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
@Override public void onSharingStarted() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onSharingStarted, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onSharingAborted() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onSharingAborted, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onSharingError(int error) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(error);
mRemote.transact(Stub.TRANSACTION_onSharingError, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onSharingProgress(long currentSize, long totalSize) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(currentSize);
_data.writeLong(totalSize);
mRemote.transact(Stub.TRANSACTION_onSharingProgress, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onGeolocShared(org.gsma.joyn.chat.Geoloc geoloc) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((geoloc!=null)) {
_data.writeInt(1);
geoloc.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onGeolocShared, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onSharingStarted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onSharingAborted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onSharingError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_onSharingProgress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_onGeolocShared = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public void onSharingStarted() throws android.os.RemoteException;
public void onSharingAborted() throws android.os.RemoteException;
public void onSharingError(int error) throws android.os.RemoteException;
public void onSharingProgress(long currentSize, long totalSize) throws android.os.RemoteException;
public void onGeolocShared(org.gsma.joyn.chat.Geoloc geoloc) throws android.os.RemoteException;
}
