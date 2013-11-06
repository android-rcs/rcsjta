/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\chat\\INewChatListener.aidl
 */
package org.gsma.joyn.chat;
/**
 * New chat invitation event listener
 */
public interface INewChatListener extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.chat.INewChatListener
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.chat.INewChatListener";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.chat.INewChatListener interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.chat.INewChatListener asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.chat.INewChatListener))) {
return ((org.gsma.joyn.chat.INewChatListener)iin);
}
return new org.gsma.joyn.chat.INewChatListener.Stub.Proxy(obj);
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
case TRANSACTION_onNewSingleChat:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.gsma.joyn.chat.ChatMessage _arg1;
if ((0!=data.readInt())) {
_arg1 = org.gsma.joyn.chat.ChatMessage.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
this.onNewSingleChat(_arg0, _arg1);
reply.writeNoException();
return true;
}
case TRANSACTION_onNewGroupChat:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
this.onNewGroupChat(_arg0);
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements org.gsma.joyn.chat.INewChatListener
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
@Override public void onNewSingleChat(java.lang.String contact, org.gsma.joyn.chat.ChatMessage message) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
if ((message!=null)) {
_data.writeInt(1);
message.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_onNewSingleChat, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void onNewGroupChat(java.lang.String chatId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(chatId);
mRemote.transact(Stub.TRANSACTION_onNewGroupChat, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_onNewSingleChat = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_onNewGroupChat = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
}
public void onNewSingleChat(java.lang.String contact, org.gsma.joyn.chat.ChatMessage message) throws android.os.RemoteException;
public void onNewGroupChat(java.lang.String chatId) throws android.os.RemoteException;
}
