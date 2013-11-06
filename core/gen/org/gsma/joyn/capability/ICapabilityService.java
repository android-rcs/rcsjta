/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\capability\\ICapabilityService.aidl
 */
package org.gsma.joyn.capability;
/**
 * Capability service API
 */
public interface ICapabilityService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.capability.ICapabilityService
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.capability.ICapabilityService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.capability.ICapabilityService interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.capability.ICapabilityService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.capability.ICapabilityService))) {
return ((org.gsma.joyn.capability.ICapabilityService)iin);
}
return new org.gsma.joyn.capability.ICapabilityService.Stub.Proxy(obj);
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
case TRANSACTION_getMyCapabilities:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.capability.Capabilities _result = this.getMyCapabilities();
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
case TRANSACTION_getContactCapabilities:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.gsma.joyn.capability.Capabilities _result = this.getContactCapabilities(_arg0);
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
case TRANSACTION_requestContactCapabilities:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.requestContactCapabilities(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_requestAllContactsCapabilities:
{
data.enforceInterface(DESCRIPTOR);
this.requestAllContactsCapabilities();
reply.writeNoException();
return true;
}
case TRANSACTION_addCapabilitiesListener:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.capability.ICapabilitiesListener _arg0;
_arg0 = org.gsma.joyn.capability.ICapabilitiesListener.Stub.asInterface(data.readStrongBinder());
this.addCapabilitiesListener(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_removeCapabilitiesListener:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.capability.ICapabilitiesListener _arg0;
_arg0 = org.gsma.joyn.capability.ICapabilitiesListener.Stub.asInterface(data.readStrongBinder());
this.removeCapabilitiesListener(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_addContactCapabilitiesListener:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.gsma.joyn.capability.ICapabilitiesListener _arg1;
_arg1 = org.gsma.joyn.capability.ICapabilitiesListener.Stub.asInterface(data.readStrongBinder());
this.addContactCapabilitiesListener(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_removeContactCapabilitiesListener:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.gsma.joyn.capability.ICapabilitiesListener _arg1;
_arg1 = org.gsma.joyn.capability.ICapabilitiesListener.Stub.asInterface(data.readStrongBinder());
this.removeContactCapabilitiesListener(_arg0, _arg1);
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
private static class Proxy implements org.gsma.joyn.capability.ICapabilityService
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
@Override public org.gsma.joyn.capability.Capabilities getMyCapabilities() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.capability.Capabilities _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getMyCapabilities, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = org.gsma.joyn.capability.Capabilities.CREATOR.createFromParcel(_reply);
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
@Override public org.gsma.joyn.capability.Capabilities getContactCapabilities(java.lang.String contact) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.capability.Capabilities _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
mRemote.transact(Stub.TRANSACTION_getContactCapabilities, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = org.gsma.joyn.capability.Capabilities.CREATOR.createFromParcel(_reply);
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
@Override public void requestContactCapabilities(java.lang.String contact) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
mRemote.transact(Stub.TRANSACTION_requestContactCapabilities, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void requestAllContactsCapabilities() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_requestAllContactsCapabilities, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void addCapabilitiesListener(org.gsma.joyn.capability.ICapabilitiesListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_addCapabilitiesListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void removeCapabilitiesListener(org.gsma.joyn.capability.ICapabilitiesListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_removeCapabilitiesListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void addContactCapabilitiesListener(java.lang.String contact, org.gsma.joyn.capability.ICapabilitiesListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_addContactCapabilitiesListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void removeContactCapabilitiesListener(java.lang.String contact, org.gsma.joyn.capability.ICapabilitiesListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_removeContactCapabilitiesListener, _data, _reply, 0);
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
static final int TRANSACTION_getMyCapabilities = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getContactCapabilities = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_requestContactCapabilities = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_requestAllContactsCapabilities = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_addCapabilitiesListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_removeCapabilitiesListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_addContactCapabilitiesListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_removeContactCapabilitiesListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_getServiceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
}
public boolean isServiceRegistered() throws android.os.RemoteException;
public void addServiceRegistrationListener(org.gsma.joyn.IJoynServiceRegistrationListener listener) throws android.os.RemoteException;
public void removeServiceRegistrationListener(org.gsma.joyn.IJoynServiceRegistrationListener listener) throws android.os.RemoteException;
public org.gsma.joyn.capability.Capabilities getMyCapabilities() throws android.os.RemoteException;
public org.gsma.joyn.capability.Capabilities getContactCapabilities(java.lang.String contact) throws android.os.RemoteException;
public void requestContactCapabilities(java.lang.String contact) throws android.os.RemoteException;
public void requestAllContactsCapabilities() throws android.os.RemoteException;
public void addCapabilitiesListener(org.gsma.joyn.capability.ICapabilitiesListener listener) throws android.os.RemoteException;
public void removeCapabilitiesListener(org.gsma.joyn.capability.ICapabilitiesListener listener) throws android.os.RemoteException;
public void addContactCapabilitiesListener(java.lang.String contact, org.gsma.joyn.capability.ICapabilitiesListener listener) throws android.os.RemoteException;
public void removeContactCapabilitiesListener(java.lang.String contact, org.gsma.joyn.capability.ICapabilitiesListener listener) throws android.os.RemoteException;
public int getServiceVersion() throws android.os.RemoteException;
}
