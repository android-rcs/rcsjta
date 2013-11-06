/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\gsh\\INewGeolocSharingListener.aidl
 */
package org.gsma.joyn.gsh;
/**
 * Callback method for new geoloc sharing invitations
 */
public interface INewGeolocSharingListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.gsh.INewGeolocSharingListener
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.gsh.INewGeolocSharingListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.gsh.INewGeolocSharingListener interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.gsh.INewGeolocSharingListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.gsh.INewGeolocSharingListener))) {
return ((org.gsma.joyn.gsh.INewGeolocSharingListener)iin);
}
return new org.gsma.joyn.gsh.INewGeolocSharingListener.Stub.Proxy(obj);
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
case TRANSACTION_onNewGeolocSharing:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.onNewGeolocSharing(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.gsma.joyn.gsh.INewGeolocSharingListener
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
@Override public void onNewGeolocSharing(java.lang.String sharingId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(sharingId);
mRemote.transact(Stub.TRANSACTION_onNewGeolocSharing, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onNewGeolocSharing = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void onNewGeolocSharing(java.lang.String sharingId) throws android.os.RemoteException;
}
