/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\ft\\IFileTransferListener.aidl
 */
package org.gsma.joyn.ft;
/**
 * Callback methods for file transfer events
 */
public interface IFileTransferListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.ft.IFileTransferListener
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.ft.IFileTransferListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.ft.IFileTransferListener interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.ft.IFileTransferListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.ft.IFileTransferListener))) {
return ((org.gsma.joyn.ft.IFileTransferListener)iin);
}
return new org.gsma.joyn.ft.IFileTransferListener.Stub.Proxy(obj);
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
case TRANSACTION_onTransferStarted:
{
data.enforceInterface(DESCRIPTOR);
this.onTransferStarted();
reply.writeNoException();
return true;
}
case TRANSACTION_onTransferAborted:
{
data.enforceInterface(DESCRIPTOR);
this.onTransferAborted();
reply.writeNoException();
return true;
}
case TRANSACTION_onTransferError:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.onTransferError(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onTransferProgress:
{
data.enforceInterface(DESCRIPTOR);
long _arg0;
_arg0 = data.readLong();
long _arg1;
_arg1 = data.readLong();
this.onTransferProgress(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_onFileTransferred:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.onFileTransferred(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.gsma.joyn.ft.IFileTransferListener
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
@Override public void onTransferStarted() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onTransferStarted, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onTransferAborted() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onTransferAborted, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onTransferError(int error) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(error);
mRemote.transact(Stub.TRANSACTION_onTransferError, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onTransferProgress(long currentSize, long totalSize) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(currentSize);
_data.writeLong(totalSize);
mRemote.transact(Stub.TRANSACTION_onTransferProgress, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onFileTransferred(java.lang.String filename) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(filename);
mRemote.transact(Stub.TRANSACTION_onFileTransferred, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onTransferStarted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onTransferAborted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onTransferError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_onTransferProgress = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_onFileTransferred = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public void onTransferStarted() throws android.os.RemoteException;
public void onTransferAborted() throws android.os.RemoteException;
public void onTransferError(int error) throws android.os.RemoteException;
public void onTransferProgress(long currentSize, long totalSize) throws android.os.RemoteException;
public void onFileTransferred(java.lang.String filename) throws android.os.RemoteException;
}
