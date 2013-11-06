/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\chat\\IGroupChatListener.aidl
 */
package org.gsma.joyn.chat;
/**
 * Group chat event listener
 */
public interface IGroupChatListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.chat.IGroupChatListener
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.chat.IGroupChatListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.chat.IGroupChatListener interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.chat.IGroupChatListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.chat.IGroupChatListener))) {
return ((org.gsma.joyn.chat.IGroupChatListener)iin);
}
return new org.gsma.joyn.chat.IGroupChatListener.Stub.Proxy(obj);
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
case TRANSACTION_onNewMessage:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.chat.ChatMessage _arg0;
if ((0!=data.readInt())) {
_arg0 = org.gsma.joyn.chat.ChatMessage.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.onNewMessage(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onReportMessageDelivered:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.onReportMessageDelivered(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onReportMessageDisplayed:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.onReportMessageDisplayed(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onReportMessageFailed:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.onReportMessageFailed(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onComposingEvent:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
boolean _arg1;
_arg1 = (0!=data.readInt());
this.onComposingEvent(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_onParticipantJoined:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
java.lang.String _arg1;
_arg1 = data.readString();
this.onParticipantJoined(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_onParticipantLeft:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.onParticipantLeft(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_onParticipantDisconnected:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.onParticipantDisconnected(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.gsma.joyn.chat.IGroupChatListener
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
@Override public void onSessionError(int reason) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(reason);
mRemote.transact(Stub.TRANSACTION_onSessionError, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onNewMessage(org.gsma.joyn.chat.ChatMessage message) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((message!=null)) {
_data.writeInt(1);
message.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onNewMessage, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onReportMessageDelivered(java.lang.String msgId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(msgId);
mRemote.transact(Stub.TRANSACTION_onReportMessageDelivered, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onReportMessageDisplayed(java.lang.String msgId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(msgId);
mRemote.transact(Stub.TRANSACTION_onReportMessageDisplayed, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onReportMessageFailed(java.lang.String msgId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(msgId);
mRemote.transact(Stub.TRANSACTION_onReportMessageFailed, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onComposingEvent(java.lang.String contact, boolean status) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
_data.writeInt(((status)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_onComposingEvent, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onParticipantJoined(java.lang.String contact, java.lang.String contactDisplayname) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
_data.writeString(contactDisplayname);
mRemote.transact(Stub.TRANSACTION_onParticipantJoined, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onParticipantLeft(java.lang.String contact) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
mRemote.transact(Stub.TRANSACTION_onParticipantLeft, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onParticipantDisconnected(java.lang.String contact) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
mRemote.transact(Stub.TRANSACTION_onParticipantDisconnected, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onSessionStarted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onSessionAborted = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_onSessionError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_onNewMessage = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_onReportMessageDelivered = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_onReportMessageDisplayed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_onReportMessageFailed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_onComposingEvent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_onParticipantJoined = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_onParticipantLeft = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_onParticipantDisconnected = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
}
public void onSessionStarted() throws android.os.RemoteException;
public void onSessionAborted() throws android.os.RemoteException;
public void onSessionError(int reason) throws android.os.RemoteException;
public void onNewMessage(org.gsma.joyn.chat.ChatMessage message) throws android.os.RemoteException;
public void onReportMessageDelivered(java.lang.String msgId) throws android.os.RemoteException;
public void onReportMessageDisplayed(java.lang.String msgId) throws android.os.RemoteException;
public void onReportMessageFailed(java.lang.String msgId) throws android.os.RemoteException;
public void onComposingEvent(java.lang.String contact, boolean status) throws android.os.RemoteException;
public void onParticipantJoined(java.lang.String contact, java.lang.String contactDisplayname) throws android.os.RemoteException;
public void onParticipantLeft(java.lang.String contact) throws android.os.RemoteException;
public void onParticipantDisconnected(java.lang.String contact) throws android.os.RemoteException;
}
