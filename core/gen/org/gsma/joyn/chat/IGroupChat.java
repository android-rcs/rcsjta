/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\chat\\IGroupChat.aidl
 */
package org.gsma.joyn.chat;
/**
 * Group chat interface
 */
public interface IGroupChat extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.chat.IGroupChat
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.chat.IGroupChat";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.chat.IGroupChat interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.chat.IGroupChat asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.chat.IGroupChat))) {
return ((org.gsma.joyn.chat.IGroupChat)iin);
}
return new org.gsma.joyn.chat.IGroupChat.Stub.Proxy(obj);
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
case TRANSACTION_getChatId:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getChatId();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getState:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getState();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getRemoteContact:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getRemoteContact();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getSubject:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getSubject();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getParticipants:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<java.lang.String> _result = this.getParticipants();
reply.writeNoException();
reply.writeStringList(_result);
return true;
}
case TRANSACTION_acceptInvitation:
{
data.enforceInterface(DESCRIPTOR);
this.acceptInvitation();
reply.writeNoException();
return true;
}
case TRANSACTION_rejectInvitation:
{
data.enforceInterface(DESCRIPTOR);
this.rejectInvitation();
reply.writeNoException();
return true;
}
case TRANSACTION_sendMessage:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _result = this.sendMessage(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_sendIsComposingEvent:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.sendIsComposingEvent(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_addParticipants:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<java.lang.String> _arg0;
_arg0 = data.createStringArrayList();
this.addParticipants(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getMaxParticipants:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getMaxParticipants();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_quitConversation:
{
data.enforceInterface(DESCRIPTOR);
this.quitConversation();
reply.writeNoException();
return true;
}
case TRANSACTION_addEventListener:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.chat.IGroupChatListener _arg0;
_arg0 = org.gsma.joyn.chat.IGroupChatListener.Stub.asInterface(data.readStrongBinder());
this.addEventListener(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_removeEventListener:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.chat.IGroupChatListener _arg0;
_arg0 = org.gsma.joyn.chat.IGroupChatListener.Stub.asInterface(data.readStrongBinder());
this.removeEventListener(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_sendGeoloc:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.chat.Geoloc _arg0;
if ((0!=data.readInt())) {
_arg0 = org.gsma.joyn.chat.Geoloc.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
java.lang.String _result = this.sendGeoloc(_arg0);
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_sendFile:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
org.gsma.joyn.ft.IFileTransferListener _arg2;
_arg2 = org.gsma.joyn.ft.IFileTransferListener.Stub.asInterface(data.readStrongBinder());
org.gsma.joyn.ft.IFileTransfer _result = this.sendFile(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeStrongBinder((((_result!=null))?(_result.asBinder()):(null)));
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
private static class Proxy implements org.gsma.joyn.chat.IGroupChat
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
@Override public java.lang.String getChatId() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getChatId, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public int getState() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getState, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String getRemoteContact() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getRemoteContact, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.lang.String getSubject() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getSubject, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.util.List<java.lang.String> getParticipants() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<java.lang.String> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getParticipants, _data, _reply, 0);
_reply.readException();
_result = _reply.createStringArrayList();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void acceptInvitation() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_acceptInvitation, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void rejectInvitation() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_rejectInvitation, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public java.lang.String sendMessage(java.lang.String text) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(text);
mRemote.transact(Stub.TRANSACTION_sendMessage, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void sendIsComposingEvent(boolean status) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((status)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_sendIsComposingEvent, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void addParticipants(java.util.List<java.lang.String> participants) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStringList(participants);
mRemote.transact(Stub.TRANSACTION_addParticipants, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public int getMaxParticipants() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getMaxParticipants, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void quitConversation() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_quitConversation, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void addEventListener(org.gsma.joyn.chat.IGroupChatListener listener) throws android.os.RemoteException
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
@Override public void removeEventListener(org.gsma.joyn.chat.IGroupChatListener listener) throws android.os.RemoteException
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
@Override public java.lang.String sendGeoloc(org.gsma.joyn.chat.Geoloc geoloc) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((geoloc!=null)) {
_data.writeInt(1);
geoloc.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_sendGeoloc, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.gsma.joyn.ft.IFileTransfer sendFile(java.lang.String filename, java.lang.String fileicon, org.gsma.joyn.ft.IFileTransferListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.ft.IFileTransfer _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(filename);
_data.writeString(fileicon);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_sendFile, _data, _reply, 0);
_reply.readException();
_result = org.gsma.joyn.ft.IFileTransfer.Stub.asInterface(_reply.readStrongBinder());
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
static final int TRANSACTION_getChatId = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_getState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_getRemoteContact = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_getSubject = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_getParticipants = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_acceptInvitation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_rejectInvitation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_sendMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_sendIsComposingEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_addParticipants = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_getMaxParticipants = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_quitConversation = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_addEventListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_removeEventListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_sendGeoloc = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
static final int TRANSACTION_sendFile = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
static final int TRANSACTION_getServiceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
}
public java.lang.String getChatId() throws android.os.RemoteException;
public int getState() throws android.os.RemoteException;
public java.lang.String getRemoteContact() throws android.os.RemoteException;
public java.lang.String getSubject() throws android.os.RemoteException;
public java.util.List<java.lang.String> getParticipants() throws android.os.RemoteException;
public void acceptInvitation() throws android.os.RemoteException;
public void rejectInvitation() throws android.os.RemoteException;
public java.lang.String sendMessage(java.lang.String text) throws android.os.RemoteException;
public void sendIsComposingEvent(boolean status) throws android.os.RemoteException;
public void addParticipants(java.util.List<java.lang.String> participants) throws android.os.RemoteException;
public int getMaxParticipants() throws android.os.RemoteException;
public void quitConversation() throws android.os.RemoteException;
public void addEventListener(org.gsma.joyn.chat.IGroupChatListener listener) throws android.os.RemoteException;
public void removeEventListener(org.gsma.joyn.chat.IGroupChatListener listener) throws android.os.RemoteException;
public java.lang.String sendGeoloc(org.gsma.joyn.chat.Geoloc geoloc) throws android.os.RemoteException;
public org.gsma.joyn.ft.IFileTransfer sendFile(java.lang.String filename, java.lang.String fileicon, org.gsma.joyn.ft.IFileTransferListener listener) throws android.os.RemoteException;
public int getServiceVersion() throws android.os.RemoteException;
}
