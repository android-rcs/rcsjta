/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\ipcall\\IIPCallRendererListener.aidl
 */
package org.gsma.joyn.ipcall;
/**
 * IP call renderer event listener interface
 */
public interface IIPCallRendererListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.ipcall.IIPCallRendererListener
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.ipcall.IIPCallRendererListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.ipcall.IIPCallRendererListener interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.ipcall.IIPCallRendererListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.ipcall.IIPCallRendererListener))) {
return ((org.gsma.joyn.ipcall.IIPCallRendererListener)iin);
}
return new org.gsma.joyn.ipcall.IIPCallRendererListener.Stub.Proxy(obj);
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
case TRANSACTION_onRendererOpened:
{
data.enforceInterface(DESCRIPTOR);
this.onRendererOpened();
reply.writeNoException();
return true;
}
case TRANSACTION_onRendererStarted:
{
data.enforceInterface(DESCRIPTOR);
this.onRendererStarted();
reply.writeNoException();
return true;
}
case TRANSACTION_onRendererStopped:
{
data.enforceInterface(DESCRIPTOR);
this.onRendererStopped();
reply.writeNoException();
return true;
}
case TRANSACTION_onRendererClosed:
{
data.enforceInterface(DESCRIPTOR);
this.onRendererClosed();
reply.writeNoException();
return true;
}
case TRANSACTION_onRendererError:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.onRendererError(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.gsma.joyn.ipcall.IIPCallRendererListener
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
@Override public void onRendererOpened() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onRendererOpened, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onRendererStarted() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onRendererStarted, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onRendererStopped() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onRendererStopped, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onRendererClosed() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onRendererClosed, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onRendererError(int error) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(error);
mRemote.transact(Stub.TRANSACTION_onRendererError, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onRendererOpened = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onRendererStarted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onRendererStopped = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_onRendererClosed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_onRendererError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public void onRendererOpened() throws android.os.RemoteException;
public void onRendererStarted() throws android.os.RemoteException;
public void onRendererStopped() throws android.os.RemoteException;
public void onRendererClosed() throws android.os.RemoteException;
public void onRendererError(int error) throws android.os.RemoteException;
}
