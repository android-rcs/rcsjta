/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: C:\\Dev\\workspace\\RCS_API\\rcsjta\\core\\src\\org\\gsma\\joyn\\chat\\IChatService.aidl
 */
package org.gsma.joyn.chat;
/**
 * Chat service API
 */
public interface IChatService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements org.gsma.joyn.chat.IChatService
{
private static final java.lang.String DESCRIPTOR = "org.gsma.joyn.chat.IChatService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an org.gsma.joyn.chat.IChatService interface,
 * generating a proxy if needed.
 */
public static org.gsma.joyn.chat.IChatService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof org.gsma.joyn.chat.IChatService))) {
return ((org.gsma.joyn.chat.IChatService)iin);
}
return new org.gsma.joyn.chat.IChatService.Stub.Proxy(obj);
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
org.gsma.joyn.chat.ChatServiceConfiguration _result = this.getConfiguration();
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
case TRANSACTION_openSingleChat:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.gsma.joyn.chat.IChatListener _arg1;
_arg1 = org.gsma.joyn.chat.IChatListener.Stub.asInterface(data.readStrongBinder());
org.gsma.joyn.chat.IChat _result = this.openSingleChat(_arg0, _arg1);
reply.writeNoException();
reply.writeStrongBinder((((_result!=null))?(_result.asBinder()):(null)));
return true;
}
case TRANSACTION_initiateGroupChat:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<java.lang.String> _arg0;
_arg0 = data.createStringArrayList();
java.lang.String _arg1;
_arg1 = data.readString();
org.gsma.joyn.chat.IGroupChatListener _arg2;
_arg2 = org.gsma.joyn.chat.IGroupChatListener.Stub.asInterface(data.readStrongBinder());
org.gsma.joyn.chat.IGroupChat _result = this.initiateGroupChat(_arg0, _arg1, _arg2);
reply.writeNoException();
reply.writeStrongBinder((((_result!=null))?(_result.asBinder()):(null)));
return true;
}
case TRANSACTION_rejoinGroupChat:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.gsma.joyn.chat.IGroupChat _result = this.rejoinGroupChat(_arg0);
reply.writeNoException();
reply.writeStrongBinder((((_result!=null))?(_result.asBinder()):(null)));
return true;
}
case TRANSACTION_restartGroupChat:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.gsma.joyn.chat.IGroupChat _result = this.restartGroupChat(_arg0);
reply.writeNoException();
reply.writeStrongBinder((((_result!=null))?(_result.asBinder()):(null)));
return true;
}
case TRANSACTION_addNewChatListener:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.chat.INewChatListener _arg0;
_arg0 = org.gsma.joyn.chat.INewChatListener.Stub.asInterface(data.readStrongBinder());
this.addNewChatListener(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_removeNewChatListener:
{
data.enforceInterface(DESCRIPTOR);
org.gsma.joyn.chat.INewChatListener _arg0;
_arg0 = org.gsma.joyn.chat.INewChatListener.Stub.asInterface(data.readStrongBinder());
this.removeNewChatListener(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getChat:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.gsma.joyn.chat.IChat _result = this.getChat(_arg0);
reply.writeNoException();
reply.writeStrongBinder((((_result!=null))?(_result.asBinder()):(null)));
return true;
}
case TRANSACTION_getChats:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<android.os.IBinder> _result = this.getChats();
reply.writeNoException();
reply.writeBinderList(_result);
return true;
}
case TRANSACTION_getGroupChats:
{
data.enforceInterface(DESCRIPTOR);
java.util.List<android.os.IBinder> _result = this.getGroupChats();
reply.writeNoException();
reply.writeBinderList(_result);
return true;
}
case TRANSACTION_getGroupChat:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
org.gsma.joyn.chat.IGroupChat _result = this.getGroupChat(_arg0);
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
private static class Proxy implements org.gsma.joyn.chat.IChatService
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
@Override public org.gsma.joyn.chat.ChatServiceConfiguration getConfiguration() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.chat.ChatServiceConfiguration _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getConfiguration, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = org.gsma.joyn.chat.ChatServiceConfiguration.CREATOR.createFromParcel(_reply);
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
@Override public org.gsma.joyn.chat.IChat openSingleChat(java.lang.String contact, org.gsma.joyn.chat.IChatListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.chat.IChat _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_openSingleChat, _data, _reply, 0);
_reply.readException();
_result = org.gsma.joyn.chat.IChat.Stub.asInterface(_reply.readStrongBinder());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.gsma.joyn.chat.IGroupChat initiateGroupChat(java.util.List<java.lang.String> contacts, java.lang.String subject, org.gsma.joyn.chat.IGroupChatListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.chat.IGroupChat _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStringList(contacts);
_data.writeString(subject);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_initiateGroupChat, _data, _reply, 0);
_reply.readException();
_result = org.gsma.joyn.chat.IGroupChat.Stub.asInterface(_reply.readStrongBinder());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.gsma.joyn.chat.IGroupChat rejoinGroupChat(java.lang.String chatId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.chat.IGroupChat _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(chatId);
mRemote.transact(Stub.TRANSACTION_rejoinGroupChat, _data, _reply, 0);
_reply.readException();
_result = org.gsma.joyn.chat.IGroupChat.Stub.asInterface(_reply.readStrongBinder());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.gsma.joyn.chat.IGroupChat restartGroupChat(java.lang.String chatId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.chat.IGroupChat _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(chatId);
mRemote.transact(Stub.TRANSACTION_restartGroupChat, _data, _reply, 0);
_reply.readException();
_result = org.gsma.joyn.chat.IGroupChat.Stub.asInterface(_reply.readStrongBinder());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public void addNewChatListener(org.gsma.joyn.chat.INewChatListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_addNewChatListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public void removeNewChatListener(org.gsma.joyn.chat.INewChatListener listener) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeStrongBinder((((listener!=null))?(listener.asBinder()):(null)));
mRemote.transact(Stub.TRANSACTION_removeNewChatListener, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
@Override public org.gsma.joyn.chat.IChat getChat(java.lang.String contact) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.chat.IChat _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(contact);
mRemote.transact(Stub.TRANSACTION_getChat, _data, _reply, 0);
_reply.readException();
_result = org.gsma.joyn.chat.IChat.Stub.asInterface(_reply.readStrongBinder());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.util.List<android.os.IBinder> getChats() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<android.os.IBinder> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getChats, _data, _reply, 0);
_reply.readException();
_result = _reply.createBinderArrayList();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public java.util.List<android.os.IBinder> getGroupChats() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.util.List<android.os.IBinder> _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getGroupChats, _data, _reply, 0);
_reply.readException();
_result = _reply.createBinderArrayList();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
@Override public org.gsma.joyn.chat.IGroupChat getGroupChat(java.lang.String chatId) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
org.gsma.joyn.chat.IGroupChat _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(chatId);
mRemote.transact(Stub.TRANSACTION_getGroupChat, _data, _reply, 0);
_reply.readException();
_result = org.gsma.joyn.chat.IGroupChat.Stub.asInterface(_reply.readStrongBinder());
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
static final int TRANSACTION_isServiceRegistered = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_addServiceRegistrationListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_removeServiceRegistrationListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_getConfiguration = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_openSingleChat = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_initiateGroupChat = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_rejoinGroupChat = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_restartGroupChat = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_addNewChatListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_removeNewChatListener = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_getChat = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_getChats = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_getGroupChats = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_getGroupChat = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_getServiceVersion = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
}
public boolean isServiceRegistered() throws android.os.RemoteException;
public void addServiceRegistrationListener(org.gsma.joyn.IJoynServiceRegistrationListener listener) throws android.os.RemoteException;
public void removeServiceRegistrationListener(org.gsma.joyn.IJoynServiceRegistrationListener listener) throws android.os.RemoteException;
public org.gsma.joyn.chat.ChatServiceConfiguration getConfiguration() throws android.os.RemoteException;
public org.gsma.joyn.chat.IChat openSingleChat(java.lang.String contact, org.gsma.joyn.chat.IChatListener listener) throws android.os.RemoteException;
public org.gsma.joyn.chat.IGroupChat initiateGroupChat(java.util.List<java.lang.String> contacts, java.lang.String subject, org.gsma.joyn.chat.IGroupChatListener listener) throws android.os.RemoteException;
public org.gsma.joyn.chat.IGroupChat rejoinGroupChat(java.lang.String chatId) throws android.os.RemoteException;
public org.gsma.joyn.chat.IGroupChat restartGroupChat(java.lang.String chatId) throws android.os.RemoteException;
public void addNewChatListener(org.gsma.joyn.chat.INewChatListener listener) throws android.os.RemoteException;
public void removeNewChatListener(org.gsma.joyn.chat.INewChatListener listener) throws android.os.RemoteException;
public org.gsma.joyn.chat.IChat getChat(java.lang.String contact) throws android.os.RemoteException;
public java.util.List<android.os.IBinder> getChats() throws android.os.RemoteException;
public java.util.List<android.os.IBinder> getGroupChats() throws android.os.RemoteException;
public org.gsma.joyn.chat.IGroupChat getGroupChat(java.lang.String chatId) throws android.os.RemoteException;
public int getServiceVersion() throws android.os.RemoteException;
}
