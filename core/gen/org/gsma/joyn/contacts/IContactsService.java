/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\contacts\\IContactsService.aidl
 */
package org.gsma.joyn.contacts;
/**
 * Contacts service API
 */
public interface IContactsService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.contacts.IContactsService
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.contacts.IContactsService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.contacts.IContactsService interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.contacts.IContactsService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.contacts.IContactsService))) {
return ((org.gsma.joyn.contacts.IContactsService)iin);
}
return new org.gsma.joyn.contacts.IContactsService.Stub.Proxy(obj);
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
case TRANSACTION_getJoynContact:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.gsma.joyn.contacts.JoynContact _result = this.getJoynContact(_arg0);
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
case TRANSACTION_getJoynContacts:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<org.gsma.joyn.contacts.JoynContact> _result = this.getJoynContacts();
reply.writeNoException();
reply.writeTypedList(_result);
return true;
}
case TRANSACTION_getJoynContactsOnline:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<org.gsma.joyn.contacts.JoynContact> _result = this.getJoynContactsOnline();
reply.writeNoException();
reply.writeTypedList(_result);
return true;
}
case TRANSACTION_getJoynContactsSupporting:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.util.List<org.gsma.joyn.contacts.JoynContact> _result = this.getJoynContactsSupporting(_arg0);
reply.writeNoException();
reply.writeTypedList(_result);
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
private static class Proxy implements org.gsma.joyn.contacts.IContactsService
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
@Override public org.gsma.joyn.contacts.JoynContact getJoynContact(java.lang.String contactId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.contacts.JoynContact _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contactId);
mRemote.transact(Stub.TRANSACTION_getJoynContact, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = org.gsma.joyn.contacts.JoynContact.CREATOR.createFromParcel(_reply);
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
@Override public java.util.List<org.gsma.joyn.contacts.JoynContact> getJoynContacts() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<org.gsma.joyn.contacts.JoynContact> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getJoynContacts, _data, _reply, 0);
_reply.readException();
_result = _reply.createTypedArrayList(org.gsma.joyn.contacts.JoynContact.CREATOR);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.util.List<org.gsma.joyn.contacts.JoynContact> getJoynContactsOnline() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<org.gsma.joyn.contacts.JoynContact> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getJoynContactsOnline, _data, _reply, 0);
_reply.readException();
_result = _reply.createTypedArrayList(org.gsma.joyn.contacts.JoynContact.CREATOR);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.util.List<org.gsma.joyn.contacts.JoynContact> getJoynContactsSupporting(java.lang.String tag) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<org.gsma.joyn.contacts.JoynContact> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(tag);
mRemote.transact(Stub.TRANSACTION_getJoynContactsSupporting, _data, _reply, 0);
_reply.readException();
_result = _reply.createTypedArrayList(org.gsma.joyn.contacts.JoynContact.CREATOR);
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
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
static final int TRANSACTION_getJoynContact = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getJoynContacts = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_getJoynContactsOnline = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_getJoynContactsSupporting = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getServiceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
}
public org.gsma.joyn.contacts.JoynContact getJoynContact(java.lang.String contactId) throws android.os.RemoteException;
public java.util.List<org.gsma.joyn.contacts.JoynContact> getJoynContacts() throws android.os.RemoteException;
public java.util.List<org.gsma.joyn.contacts.JoynContact> getJoynContactsOnline() throws android.os.RemoteException;
public java.util.List<org.gsma.joyn.contacts.JoynContact> getJoynContactsSupporting(java.lang.String tag) throws android.os.RemoteException;
public int getServiceVersion() throws android.os.RemoteException;
}
