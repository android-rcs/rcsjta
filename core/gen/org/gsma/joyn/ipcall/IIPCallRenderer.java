/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\ipcall\\IIPCallRenderer.aidl
 */
package org.gsma.joyn.ipcall;
/**
 * IP call renderer interface
 */
public interface IIPCallRenderer extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.ipcall.IIPCallRenderer
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.ipcall.IIPCallRenderer";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.ipcall.IIPCallRenderer interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.ipcall.IIPCallRenderer asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.ipcall.IIPCallRenderer))) {
return ((org.gsma.joyn.ipcall.IIPCallRenderer)iin);
}
return new org.gsma.joyn.ipcall.IIPCallRenderer.Stub.Proxy(obj);
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
case TRANSACTION_open:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.ipcall.AudioCodec _arg0;
if ((0!=data.readInt())) {
_arg0 = org.gsma.joyn.ipcall.AudioCodec.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
org.gsma.joyn.ipcall.VideoCodec _arg1;
if ((0!=data.readInt())) {
_arg1 = org.gsma.joyn.ipcall.VideoCodec.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
java.lang.String _arg2;
_arg2 = data.readString();
int _arg3;
_arg3 = data.readInt();
int _arg4;
_arg4 = data.readInt();
this.open(_arg0, _arg1, _arg2, _arg3, _arg4);
reply.writeNoException();
return true;
}
case TRANSACTION_close:
{
data.enforceInterface(DESCRIPTOR);
this.close();
reply.writeNoException();
return true;
}
case TRANSACTION_start:
{
data.enforceInterface(DESCRIPTOR);
this.start();
reply.writeNoException();
return true;
}
case TRANSACTION_stop:
{
data.enforceInterface(DESCRIPTOR);
this.stop();
reply.writeNoException();
return true;
}
case TRANSACTION_getLocalAudioRtpPort:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getLocalAudioRtpPort();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getAudioCodec:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.ipcall.AudioCodec _result = this.getAudioCodec();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_getSupportedAudioCodecs:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.ipcall.AudioCodec[] _result = this.getSupportedAudioCodecs();
reply.writeNoException();
reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
return true;
}
case TRANSACTION_getLocalVideoRtpPort:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getLocalVideoRtpPort();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getVideoCodec:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.ipcall.VideoCodec _result = this.getVideoCodec();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_getSupportedVideoCodecs:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.ipcall.VideoCodec[] _result = this.getSupportedVideoCodecs();
reply.writeNoException();
reply.writeTypedArray(_result, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
return true;
}
case TRANSACTION_addEventListener:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.ipcall.IIPCallRendererListener _arg0;
_arg0 = org.gsma.joyn.ipcall.IIPCallRendererListener.Stub.asInterface(data.readStrongBinder());
this.addEventListener(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_removeEventListener:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.ipcall.IIPCallRendererListener _arg0;
_arg0 = org.gsma.joyn.ipcall.IIPCallRendererListener.Stub.asInterface(data.readStrongBinder());
this.removeEventListener(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.gsma.joyn.ipcall.IIPCallRenderer
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
@Override public void open(org.gsma.joyn.ipcall.AudioCodec audiocodec, org.gsma.joyn.ipcall.VideoCodec videocodec, java.lang.String remoteHost, int remoteAudioPort, int remoteVideoPort) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((audiocodec!=null)) {
_data.writeInt(1);
audiocodec.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
if ((videocodec!=null)) {
_data.writeInt(1);
videocodec.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
_data.writeString(remoteHost);
_data.writeInt(remoteAudioPort);
_data.writeInt(remoteVideoPort);
mRemote.transact(Stub.TRANSACTION_open, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void close() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_close, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void start() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_start, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void stop() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int getLocalAudioRtpPort() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLocalAudioRtpPort, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.gsma.joyn.ipcall.AudioCodec getAudioCodec() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.ipcall.AudioCodec _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getAudioCodec, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = org.gsma.joyn.ipcall.AudioCodec.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.gsma.joyn.ipcall.AudioCodec[] getSupportedAudioCodecs() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.ipcall.AudioCodec[] _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getSupportedAudioCodecs, _data, _reply, 0);
_reply.readException();
_result = _reply.createTypedArray(org.gsma.joyn.ipcall.AudioCodec.CREATOR);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public int getLocalVideoRtpPort() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLocalVideoRtpPort, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.gsma.joyn.ipcall.VideoCodec getVideoCodec() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.ipcall.VideoCodec _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getVideoCodec, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = org.gsma.joyn.ipcall.VideoCodec.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.gsma.joyn.ipcall.VideoCodec[] getSupportedVideoCodecs() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.ipcall.VideoCodec[] _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getSupportedVideoCodecs, _data, _reply, 0);
_reply.readException();
_result = _reply.createTypedArray(org.gsma.joyn.ipcall.VideoCodec.CREATOR);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void addEventListener(org.gsma.joyn.ipcall.IIPCallRendererListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_addEventListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void removeEventListener(org.gsma.joyn.ipcall.IIPCallRendererListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_removeEventListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_open = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_close = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_start = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_stop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getLocalAudioRtpPort = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_getAudioCodec = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_getSupportedAudioCodecs = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_getLocalVideoRtpPort = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_getVideoCodec = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_getSupportedVideoCodecs = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_addEventListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_removeEventListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
}
public void open(org.gsma.joyn.ipcall.AudioCodec audiocodec, org.gsma.joyn.ipcall.VideoCodec videocodec, java.lang.String remoteHost, int remoteAudioPort, int remoteVideoPort) throws android.os.RemoteException;
public void close() throws android.os.RemoteException;
public void start() throws android.os.RemoteException;
public void stop() throws android.os.RemoteException;
public int getLocalAudioRtpPort() throws android.os.RemoteException;
public org.gsma.joyn.ipcall.AudioCodec getAudioCodec() throws android.os.RemoteException;
public org.gsma.joyn.ipcall.AudioCodec[] getSupportedAudioCodecs() throws android.os.RemoteException;
public int getLocalVideoRtpPort() throws android.os.RemoteException;
public org.gsma.joyn.ipcall.VideoCodec getVideoCodec() throws android.os.RemoteException;
public org.gsma.joyn.ipcall.VideoCodec[] getSupportedVideoCodecs() throws android.os.RemoteException;
public void addEventListener(org.gsma.joyn.ipcall.IIPCallRendererListener listener) throws android.os.RemoteException;
public void removeEventListener(org.gsma.joyn.ipcall.IIPCallRendererListener listener) throws android.os.RemoteException;
}
