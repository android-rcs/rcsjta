/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\session\\IMultimediaSessionListener.aidl
 */
package org.gsma.joyn.session;
/**
 * Callback methods for multimedia session events
 */
public interface IMultimediaSessionListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.session.IMultimediaSessionListener
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.session.IMultimediaSessionListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.session.IMultimediaSessionListener interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.session.IMultimediaSessionListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.session.IMultimediaSessionListener))) {
return ((org.gsma.joyn.session.IMultimediaSessionListener)iin);
}
return new org.gsma.joyn.session.IMultimediaSessionListener.Stub.Proxy(obj);
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
case TRANSACTION_onSessionStarted:
{
data.enforceInterface(DESCRIPTOR);
this.onSessionStarted();
reply.writeNoException();
return true;
}
case TRANSACTION_onSessionRinging:
{
data.enforceInterface(DESCRIPTOR);
this.onSessionRinging();
reply.writeNoException();
return true;
}
case TRANSACTION_onSessionAborted:
{
data.enforceInterface(DESCRIPTOR);
this.onSessionAborted();
reply.writeNoException();
return true;
}
case TRANSACTION_onSessionError:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.onSessionError(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.gsma.joyn.session.IMultimediaSessionListener
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
@Override public void onSessionStarted() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onSessionStarted, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onSessionRinging() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onSessionRinging, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onSessionAborted() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_onSessionAborted, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onSessionError(int error) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(error);
mRemote.transact(Stub.TRANSACTION_onSessionError, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onSessionStarted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onSessionRinging = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onSessionAborted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_onSessionError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
}
public void onSessionStarted() throws android.os.RemoteException;
public void onSessionRinging() throws android.os.RemoteException;
public void onSessionAborted() throws android.os.RemoteException;
public void onSessionError(int error) throws android.os.RemoteException;
}
