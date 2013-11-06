/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\ish\\IImageSharingService.aidl
 */
package org.gsma.joyn.ish;
/**
 * Image sharing service API
 */
public interface IImageSharingService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.ish.IImageSharingService
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.ish.IImageSharingService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.ish.IImageSharingService interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.ish.IImageSharingService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.ish.IImageSharingService))) {
return ((org.gsma.joyn.ish.IImageSharingService)iin);
}
return new org.gsma.joyn.ish.IImageSharingService.Stub.Proxy(obj);
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
case TRANSACTION_isServiceRegistered:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isServiceRegistered();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_addServiceRegistrationListener:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.IJoynServiceRegistrationListener _arg0;
_arg0 = org.gsma.joyn.IJoynServiceRegistrationListener.Stub.asInterface(data.readStrongBinder());
this.addServiceRegistrationListener(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_removeServiceRegistrationListener:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.IJoynServiceRegistrationListener _arg0;
_arg0 = org.gsma.joyn.IJoynServiceRegistrationListener.Stub.asInterface(data.readStrongBinder());
this.removeServiceRegistrationListener(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getConfiguration:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.ish.ImageSharingServiceConfiguration _result = this.getConfiguration();
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
case TRANSACTION_getImageSharings:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<android.os.IBinder> _result = this.getImageSharings();
reply.writeNoException();
reply.writeBinderList(_result);
return true;
}
case TRANSACTION_getImageSharing:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.gsma.joyn.ish.IImageSharing _result = this.getImageSharing(_arg0);
reply.writeNoException();
reply.writeStrongBinder((((_result!=null))?(_result.asBinder()):(null)));
return true;
}
case TRANSACTION_shareImage:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
org.gsma.joyn.ish.IImageSharingListener _arg2;
_arg2 = org.gsma.joyn.ish.IImageSharingListener.Stub.asInterface(data.readStrongBinder());
org.gsma.joyn.ish.IImageSharing _result = this.shareImage(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeStrongBinder((((_result!=null))?(_result.asBinder()):(null)));
return true;
}
case TRANSACTION_addNewImageSharingListener:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.ish.INewImageSharingListener _arg0;
_arg0 = org.gsma.joyn.ish.INewImageSharingListener.Stub.asInterface(data.readStrongBinder());
this.addNewImageSharingListener(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_removeNewImageSharingListener:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.ish.INewImageSharingListener _arg0;
_arg0 = org.gsma.joyn.ish.INewImageSharingListener.Stub.asInterface(data.readStrongBinder());
this.removeNewImageSharingListener(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getServiceVersion:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getServiceVersion();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.gsma.joyn.ish.IImageSharingService
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
@Override public boolean isServiceRegistered() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isServiceRegistered, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void addServiceRegistrationListener(org.gsma.joyn.IJoynServiceRegistrationListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_addServiceRegistrationListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void removeServiceRegistrationListener(org.gsma.joyn.IJoynServiceRegistrationListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_removeServiceRegistrationListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public org.gsma.joyn.ish.ImageSharingServiceConfiguration getConfiguration() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.ish.ImageSharingServiceConfiguration _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getConfiguration, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = org.gsma.joyn.ish.ImageSharingServiceConfiguration.CREATOR.createFromParcel(_reply);
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
@Override public java.util.List<android.os.IBinder> getImageSharings() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<android.os.IBinder> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getImageSharings, _data, _reply, 0);
_reply.readException();
_result = _reply.createBinderArrayList();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.gsma.joyn.ish.IImageSharing getImageSharing(java.lang.String sharingId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.ish.IImageSharing _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(sharingId);
mRemote.transact(Stub.TRANSACTION_getImageSharing, _data, _reply, 0);
_reply.readException();
_result = org.gsma.joyn.ish.IImageSharing.Stub.asInterface(_reply.readStrongBinder());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.gsma.joyn.ish.IImageSharing shareImage(java.lang.String contact, java.lang.String filename, org.gsma.joyn.ish.IImageSharingListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.ish.IImageSharing _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
_data.writeString(filename);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_shareImage, _data, _reply, 0);
_reply.readException();
_result = org.gsma.joyn.ish.IImageSharing.Stub.asInterface(_reply.readStrongBinder());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void addNewImageSharingListener(org.gsma.joyn.ish.INewImageSharingListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_addNewImageSharingListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void removeNewImageSharingListener(org.gsma.joyn.ish.INewImageSharingListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_removeNewImageSharingListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int getServiceVersion() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getServiceVersion, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_isServiceRegistered = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_addServiceRegistrationListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_removeServiceRegistrationListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_getConfiguration = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getImageSharings = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_getImageSharing = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_shareImage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_addNewImageSharingListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_removeNewImageSharingListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_getServiceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
}
public boolean isServiceRegistered() throws android.os.RemoteException;
public void addServiceRegistrationListener(org.gsma.joyn.IJoynServiceRegistrationListener listener) throws android.os.RemoteException;
public void removeServiceRegistrationListener(org.gsma.joyn.IJoynServiceRegistrationListener listener) throws android.os.RemoteException;
public org.gsma.joyn.ish.ImageSharingServiceConfiguration getConfiguration() throws android.os.RemoteException;
public java.util.List<android.os.IBinder> getImageSharings() throws android.os.RemoteException;
public org.gsma.joyn.ish.IImageSharing getImageSharing(java.lang.String sharingId) throws android.os.RemoteException;
public org.gsma.joyn.ish.IImageSharing shareImage(java.lang.String contact, java.lang.String filename, org.gsma.joyn.ish.IImageSharingListener listener) throws android.os.RemoteException;
public void addNewImageSharingListener(org.gsma.joyn.ish.INewImageSharingListener listener) throws android.os.RemoteException;
public void removeNewImageSharingListener(org.gsma.joyn.ish.INewImageSharingListener listener) throws android.os.RemoteException;
public int getServiceVersion() throws android.os.RemoteException;
}
