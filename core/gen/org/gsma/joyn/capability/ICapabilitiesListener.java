/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\capability\\ICapabilitiesListener.aidl
 */
package org.gsma.joyn.capability;
/**
 * Callback method for new capabilities
 */
public interface ICapabilitiesListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.capability.ICapabilitiesListener
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.capability.ICapabilitiesListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.capability.ICapabilitiesListener interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.capability.ICapabilitiesListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.capability.ICapabilitiesListener))) {
return ((org.gsma.joyn.capability.ICapabilitiesListener)iin);
}
return new org.gsma.joyn.capability.ICapabilitiesListener.Stub.Proxy(obj);
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
case TRANSACTION_onCapabilitiesReceived:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.gsma.joyn.capability.Capabilities _arg1;
if ((0!=data.readInt())) {
_arg1 = org.gsma.joyn.capability.Capabilities.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
this.onCapabilitiesReceived(_arg0, _arg1);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.gsma.joyn.capability.ICapabilitiesListener
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
@Override public void onCapabilitiesReceived(java.lang.String contact, org.gsma.joyn.capability.Capabilities capabilities) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
if ((capabilities!=null)) {
_data.writeInt(1);
capabilities.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onCapabilitiesReceived, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onCapabilitiesReceived = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
}
public void onCapabilitiesReceived(java.lang.String contact, org.gsma.joyn.capability.Capabilities capabilities) throws android.os.RemoteException;
}
