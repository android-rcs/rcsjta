/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.tests.sigtest;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents class descriptions loaded from a jdiff xml file.  Used
 * for CTS SignatureTests.
 */
public class JDiffClassDescription {
    /** Indicates that the class is an annotation. */
    private static final int CLASS_MODIFIER_ANNOTATION = 0x00002000;
    /** Indicates that the class is an enum. */
    private static final int CLASS_MODIFIER_ENUM       = 0x00004000;

    /** Indicates that the method is a bridge method. */
    private static final int METHOD_MODIFIER_BRIDGE    = 0x00000040;
    /** Indicates that the method is takes a variable number of arguments. */
    private static final int METHOD_MODIFIER_VAR_ARGS  = 0x00000080;
    /** Indicates that the method is a synthetic method. */
    private static final int METHOD_MODIFIER_SYNTHETIC = 0x00001000;

    public enum JDiffType {
        INTERFACE, CLASS
    }

    @SuppressWarnings("unchecked")
    private Class<?> mClass;

    private String mPackageName;
    private String mShortClassName;

    /**
     * Package name + short class name
     */
    private String mAbsoluteClassName;

    private int mModifier;

    private String mExtendedClass;
    private List<String> implInterfaces = new ArrayList<String>();
    private List<JDiffField> jDiffFields = new ArrayList<JDiffField>();
    private List<JDiffMethod> jDiffMethods = new ArrayList<JDiffMethod>();
    private List<JDiffConstructor> jDiffConstructors = new ArrayList<JDiffConstructor>();

    private ResultObserver mResultObserver;
    private JDiffType mClassType;

    /**
     * Creates a new JDiffClassDescription.
     *
     * @param pkg the java package this class will end up in.
     * @param className the name of the class.
     */
    public JDiffClassDescription(String pkg, String className) {
        this(pkg, className, new ResultObserver() {
            public void notifyFailure(SignatureTestActivity.FAILURE_TYPE type,
                    String name,
                    String errorMessage) {
                // This is a null result observer that doesn't do anything.
            }
        });
    }

    /**
     * Creates a new JDiffClassDescription with the specified results
     * observer.
     *
     * @param pkg the java package this class belongs in.
     * @param className the name of the class.
     * @param resultObserver the resultObserver to get results with.
     */
    public JDiffClassDescription(String pkg,
            String className,
            ResultObserver resultObserver) {
        mPackageName = pkg;
        mShortClassName = className;
        mResultObserver = resultObserver;
    }

    /**
     * adds implemented interface name.
     *
     * @param iname name of interface
     */
    public void addImplInterface(String iname) {
        implInterfaces.add(iname);
    }

    /**
     * Adds a field.
     *
     * @param field the field to be added.
     */
    public void addField(JDiffField field) {
        jDiffFields.add(field);
    }

    /**
     * Adds a method.
     *
     * @param method the method to be added.
     */
    public void addMethod(JDiffMethod method) {
        jDiffMethods.add(method);
    }

    /**
     * Adds a constructor.
     *
     * @param tc the constructor to be added.
     */
    public void addConstructor(JDiffConstructor tc) {
        jDiffConstructors.add(tc);
    }

    static String convertModifiersToAccessLevel(int modifiers) {
        String accessLevel = "";
        if ((modifiers & Modifier.PUBLIC) != 0) {
            return "public";
        } else if ((modifiers & Modifier.PRIVATE) != 0) {
            return "private";
        } else if ((modifiers & Modifier.PROTECTED) != 0) {
            return "protected";
        } else {
            // package protected
            return "";
        }
    }

    static String convertModifersToModifierString(int modifiers) {
        StringBuffer sb = new StringBuffer();
        boolean isFirst = true;

        // order taken from Java Language Spec, sections 8.1.1, 8.3.1, and 8.4.3
        if ((modifiers & Modifier.ABSTRACT) != 0) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(" ");
            }
            sb.append("abstract");
        }
        if ((modifiers & Modifier.STATIC) != 0) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(" ");
            }
            sb.append("static");
        }
        if ((modifiers & Modifier.FINAL) != 0) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(" ");
            }
            sb.append("final");
        }
        if ((modifiers & Modifier.TRANSIENT) != 0) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(" ");
            }
            sb.append("transient");
        }
        if ((modifiers & Modifier.VOLATILE) != 0) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(" ");
            }
            sb.append("volatile");
        }
        if ((modifiers & Modifier.SYNCHRONIZED) != 0) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(" ");
            }
            sb.append("synchronized");
        }
        if ((modifiers & Modifier.NATIVE) != 0) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(" ");
            }
            sb.append("native");
        }
        if ((modifiers & Modifier.STRICT) != 0) {
            if (isFirst) {
                isFirst = false;
            } else {
                sb.append(" ");
            }
            sb.append("strictfp");
        }

        return sb.toString();
    }

    public abstract static class JDiffElement {
        final String mName;
        int mModifier;

        public JDiffElement(String name, int modifier) {
            mName = name;
            mModifier = modifier;
        }
    }

    /**
     * Represents a  field.
     */
    public static final class JDiffField extends JDiffElement {
        private String mFieldType;

        public JDiffField(String name, String fieldType, int modifier) {
            super(name, modifier);

            mFieldType = fieldType;
        }

        /**
         * Make a readable string according to the class name specified.
         *
         * @param className The specified class name.
         * @return A readable string to represent this field along with the class name.
         */
        public String toReadableString(String className) {
            return className + "#" + mName + "(" + mFieldType + ")";
        }

        public String toSignatureString() {
            StringBuffer sb = new StringBuffer();

            // access level
            String accesLevel = convertModifiersToAccessLevel(mModifier);
            if (!"".equals(accesLevel)) {
                sb.append(accesLevel).append(" ");
            }

            String modifierString = convertModifersToModifierString(mModifier);
            if (!"".equals(modifierString)) {
                sb.append(modifierString).append(" ");
            }

            sb.append(mFieldType).append(" ");

            sb.append(mName);

            return sb.toString();
        }
    }

    /**
     * Represents a method.
     */
    public static class JDiffMethod extends JDiffElement {
        protected String mReturnType;
        protected ArrayList<String> mParamList;
        protected ArrayList<String> mExceptionList;

        public JDiffMethod(String name, int modifier, String returnType) {
            super(name, modifier);

            if (returnType == null) {
                mReturnType = "void";
            } else {
                mReturnType = scrubJdiffParamType(returnType);
            }

            mParamList = new ArrayList<String>();
            mExceptionList = new ArrayList<String>();
        }

        /**
         * Adds a parameter.
         *
         * @param param parameter type
         */
        public void addParam(String param) {
            mParamList.add(scrubJdiffParamType(param));
        }

        /**
         * Adds an exception.
         *
         * @param exceptionName name of exception
         */
        public void addException(String exceptionName) {
            mExceptionList.add(exceptionName);
        }

        /**
         * Makes a readable string according to the class name specified.
         *
         * @param className The specified class name.
         * @return A readable string to represent this method along with the class name.
         */
        public String toReadableString(String className) {
            return className + "#" + mName + "(" + convertParamList(mParamList) + ")";
        }

        /**
         * Converts a parameter array to a string
         *
         * @param params the array to convert
         * @return converted parameter string
         */
        private static String convertParamList(final ArrayList<String> params) {

            StringBuffer paramList = new StringBuffer();

            if (params != null) {
                for (String str : params) {
                    paramList.append(str + ", ");
                }
                if (params.size() > 0) {
                    paramList.delete(paramList.length() - 2, paramList.length());
                }
            }

            return paramList.toString();
        }

        public String toSignatureString() {
            StringBuffer sb = new StringBuffer();

            // access level
            String accesLevel = convertModifiersToAccessLevel(mModifier);
            if (!"".equals(accesLevel)) {
                sb.append(accesLevel).append(" ");
            }

            String modifierString = convertModifersToModifierString(mModifier);
            if (!"".equals(modifierString)) {
                sb.append(modifierString).append(" ");
            }

            String returnType = getReturnType();
            if (!"".equals(returnType)) {
                sb.append(returnType).append(" ");
            }

            sb.append(mName);
            sb.append("(");
            for (int x = 0; x < mParamList.size(); x++) {
                sb.append(mParamList.get(x));
                if (x + 1 != mParamList.size()) {
                    sb.append(", ");
                }
            }
            sb.append(")");

            // does it throw?
            if (mExceptionList.size() > 0) {
                sb.append(" throws ");
                for (int x = 0; x < mExceptionList.size(); x++) {
                    sb.append(mExceptionList.get(x));
                    if (x + 1 != mExceptionList.size()) {
                        sb.append(", ");
                    }
                }
            }

            return sb.toString();
        }

        /**
         * Gets the return type.
         *
         * @return the return type of this method.
         */
        protected String getReturnType() {
            return mReturnType;
        }
    }

    /**
     * Represents a constructor.
     */
    public static final class JDiffConstructor extends JDiffMethod {
        public JDiffConstructor(String name, int modifier) {
            super(name, modifier, null);
        }

        public JDiffConstructor(String name, String[] param, int modifier) {
            super(name, modifier, null);

            for (int i = 0; i < param.length; i++) {
                addParam(param[i]);
            }
        }

        /**
         * Gets the return type.
         *
         * @return the return type of this method.
         */
        @Override
        protected String getReturnType() {
            // Constructors have no return type.
            return "";
        }
    }

    /**
     * Checks test class's name, modifier, fields, constructors, and
     * methods.
     */
    public void checkSignatureCompliance() {
        checkClassCompliance();
        if (mClass != null) {
            checkFieldsCompliance();
            checkConstructorCompliance();
            checkMethodCompliance();
        }
    }

    /**
     * Checks to ensure that the modifiers value for two methods are
     * compatible.
     *
     * Allowable differences are:
     *   - synchronized is allowed to be removed from an apiMethod
     *     that has it
     *   - the native modified is ignored
     *
     * @param apiMethod the method read from the api file.
     * @param reflectedMethod the method found via reflections.
     */
    private boolean areMethodModifiedCompatibile(JDiffMethod apiMethod ,
            Method reflectedMethod) {

        // If the apiMethod isn't synchronized
        if (((apiMethod.mModifier & Modifier.SYNCHRONIZED) == 0) &&
                // but the reflected method is
                ((reflectedMethod.getModifiers() & Modifier.SYNCHRONIZED) != 0)) {
            // that is a problem
            return false;
        }

        // Mask off NATIVE since it is a don't care.  Also mask off
        // SYNCHRONIZED since we've already handled that check.
        int mod1 = reflectedMethod.getModifiers() & ~(Modifier.NATIVE | Modifier.SYNCHRONIZED);
        int mod2 = apiMethod.mModifier & ~(Modifier.NATIVE | Modifier.SYNCHRONIZED);

        // We can ignore FINAL for final classes
        if ((mModifier & Modifier.FINAL) != 0) {
            mod1 &= ~Modifier.FINAL;
            mod2 &= ~Modifier.FINAL;
        }

        return mod1 == mod2;
    }

    /**
     * Checks that the method found through reflection matches the
     * specification from the API xml file.
     */
    private void checkMethodCompliance() {
        for (JDiffMethod method : jDiffMethods) {
            try {
                // this is because jdiff think a method in an interface is not abstract
                if (JDiffType.INTERFACE.equals(mClassType)) {
                    method.mModifier |= Modifier.ABSTRACT;
                }

                Method m = findMatchingMethod(method);
                if (m == null) {
                    mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISSING_METHOD,
                            method.toReadableString(mAbsoluteClassName),
                            "No method with correct signature found:" +
                            method.toSignatureString());
                } else {
                    if (m.isVarArgs()) {
                        method.mModifier |= METHOD_MODIFIER_VAR_ARGS;
                    }
                    if (m.isBridge()) {
                        method.mModifier |= METHOD_MODIFIER_BRIDGE;
                    }
                    if (m.isSynthetic()) {
                        method.mModifier |= METHOD_MODIFIER_SYNTHETIC;
                    }

                    // FIXME: A workaround to fix the final mismatch on enumeration
                    if (mClass.isEnum() && method.mName.equals("values")) {
                        return;
                    }

                    if (!areMethodModifiedCompatibile(method, m)) {
                        mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISMATCH_METHOD,
                                method.toReadableString(mAbsoluteClassName),
                                "Non-compatible method found when looking for " +
                                method.toSignatureString());
                    }
                }
            } catch (Exception e) {
                SignatureTestLog.e("Got exception when checking method compliance", e);
                mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.CAUGHT_EXCEPTION,
                        method.toReadableString(mAbsoluteClassName),
                "Exception!");
            }
        }
    }

    /**
     * Checks if the two types of methods are the same.
     *
     * @param jDiffMethod the jDiffMethod to compare
     * @param method the reflected method to compare
     * @return true, if both methods are the same
     */
    private boolean matches(JDiffMethod jDiffMethod, Method method) {
        // If the method names aren't equal, the methods can't match.
        if (jDiffMethod.mName.equals(method.getName())) {
            String jdiffReturnType = jDiffMethod.mReturnType;
            String reflectionReturnType = typeToString(method.getGenericReturnType());
            List<String> jdiffParamList = jDiffMethod.mParamList;

            // Next, compare the return types of the two methods.  If
            // they aren't equal, the methods can't match.
            if (jdiffReturnType.equals(reflectionReturnType)) {
                Type[] params = method.getGenericParameterTypes();
                // Next, check the method parameters.  If they have
                // different number of parameters, the two methods
                // can't match.
                if (jdiffParamList.size() == params.length) {
                    // If any of the parameters don't match, the
                    // methods can't match.
                    for (int i = 0; i < jdiffParamList.size(); i++) {
                        if (!compareParam(jdiffParamList.get(i), params[i])) {
                            return false;
                        }
                    }
                    // We've passed all the tests, the methods do
                    // match.
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Finds the reflected method specified by the method description.
     *
     * @param method description of the method to find
     * @return the reflected method, or null if not found.
     */
    @SuppressWarnings("unchecked")
    private Method findMatchingMethod(JDiffMethod method) {
        Method[] methods = mClass.getDeclaredMethods();
        boolean found = false;

        for (Method m : methods) {
            if (matches(method, m)) {
                return m;
            }
        }

        return null;
    }

    /**
     * Compares the parameter from the API and the parameter from
     * reflection.
     *
     * @param jdiffParam param parsed from the API xml file.
     * @param reflectionParamType param gotten from the Java reflection.
     * @return True if the two params match, otherwise return false.
     */
    private static boolean compareParam(String jdiffParam, Type reflectionParamType) {
        if (jdiffParam == null) {
            return false;
        }

        String reflectionParam = typeToString(reflectionParamType);
        // Most things aren't varargs, so just do a simple compare
        // first.
        if (jdiffParam.equals(reflectionParam)) {
            return true;
        }

        // Check for varargs.  jdiff reports varargs as ..., while
        // reflection reports them as []
        int jdiffParamEndOffset = jdiffParam.indexOf("...");
        int reflectionParamEndOffset = reflectionParam.indexOf("[]");
        if (jdiffParamEndOffset != -1 && reflectionParamEndOffset != -1) {
            jdiffParam = jdiffParam.substring(0, jdiffParamEndOffset);
            reflectionParam = reflectionParam.substring(0, reflectionParamEndOffset);
            return jdiffParam.equals(reflectionParam);
        }

        return false;
    }

    /**
     * Checks whether the constructor parsed from API xml file and
     * Java reflection are compliant.
     */
    @SuppressWarnings("unchecked")
    private void checkConstructorCompliance() {
        for (JDiffConstructor con : jDiffConstructors) {
            try {
                Constructor<?> c = findMatchingConstructor(con);
                if (c == null) {
                    mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISSING_METHOD,
                            con.toReadableString(mAbsoluteClassName),
                            "No method with correct signature found:" +
                            con.toSignatureString());
                } else {
                    if (c.isVarArgs()) {// some method's parameter are variable args
                        con.mModifier |= METHOD_MODIFIER_VAR_ARGS;
                    }
                    if (c.getModifiers() != con.mModifier) {
                        mResultObserver.notifyFailure(
                                SignatureTestActivity.FAILURE_TYPE.MISMATCH_METHOD,
                                con.toReadableString(mAbsoluteClassName),
                                "Non-compatible method found when looking for " +
                                con.toSignatureString());
                    }
                }
            } catch (Exception e) {
                SignatureTestLog.e("Got exception when checking constructor compliance", e);
                mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.CAUGHT_EXCEPTION,
                        con.toReadableString(mAbsoluteClassName),
                "Exception!");
            }
        }
    }

    /**
     * Searches available constructor.
     *
     * @param jdiffDes constructor description to find.
     * @return reflected constructor, or null if not found.
     */
    @SuppressWarnings("unchecked")
    private Constructor<?> findMatchingConstructor(JDiffConstructor jdiffDes) {
        for (Constructor<?> c : mClass.getDeclaredConstructors()) {
            Type[] params = c.getGenericParameterTypes();
            boolean isStaticClass = ((mClass.getModifiers() & Modifier.STATIC) != 0);

            int startParamOffset = 0;
            int numberOfParams = params.length;

            // non-static inner class -> skip implicit parent pointer
            // as first arg
            if (mClass.isMemberClass() && !isStaticClass && params.length >= 1) {
                startParamOffset = 1;
                --numberOfParams;
            }

            ArrayList<String> jdiffParamList = jdiffDes.mParamList;
            if (jdiffParamList.size() == numberOfParams) {
                boolean isFound = true;
                // i counts jdiff params, j counts reflected params
                int i = 0;
                int j = startParamOffset;
                while (i < jdiffParamList.size()) {
                    if (!compareParam(jdiffParamList.get(i), params[j])) {
                        isFound = false;
                        break;
                    }
                    ++i;
                    ++j;
                }
                if (isFound) {
                    return c;
                }
            }
        }
        return null;
    }

    /**
     * Checks all fields in test class for compliance with the API
     * xml.
     */
    @SuppressWarnings("unchecked")
    private void checkFieldsCompliance() {
        for (JDiffField field : jDiffFields) {
            try {
                Field f = findMatchingField(field);
                if (f == null) {
                    mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISSING_FIELD,
                            field.toReadableString(mAbsoluteClassName),
                            "No field with correct signature found:" +
                            field.toSignatureString());
                } else if (f.getModifiers() != field.mModifier) {
                    mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISMATCH_FIELD,
                            field.toReadableString(mAbsoluteClassName),
                            "Non-compatible field modifiers found when looking for " +
                            field.toSignatureString());
                } else if (!f.getType().getCanonicalName().equals(field.mFieldType)) {
                    // type name does not match, but this might be a generic
                    String genericTypeName = null;
                    Type type = f.getGenericType();
                    if (type != null) {
                        genericTypeName = type instanceof Class ? ((Class) type).getName() :
                            type.toString();
                    }
                    if (genericTypeName == null || !genericTypeName.equals(field.mFieldType)) {
                        mResultObserver.notifyFailure(
                                SignatureTestActivity.FAILURE_TYPE.MISMATCH_FIELD,
                                field.toReadableString(mAbsoluteClassName),
                                "Non-compatible field type found when looking for " +
                                field.toSignatureString());
                    }
                }

            } catch (Exception e) {
                SignatureTestLog.e("Got exception when checking field compliance", e);
                mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.CAUGHT_EXCEPTION,
                        field.toReadableString(mAbsoluteClassName),
                "Exception!");
            }
        }
    }

    /**
     * Finds the reflected field specified by the field description.
     *
     * @param field the field description to find
     * @return the reflected field, or null if not found.
     */
    private Field findMatchingField(JDiffField field){
        Field[] fields = mClass.getDeclaredFields();
        for (Field f : fields) {
            if (f.getName().equals(field.mName)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Checks if the class under test has compliant modifiers compared to the API.
     *
     * @return true if modifiers are compliant.
     */
    private boolean checkClassModifiersCompliance() {
        int reflectionModifier = mClass.getModifiers();
        int apiModifier = mModifier;

        // If the api class isn't abstract
        if (((apiModifier & Modifier.ABSTRACT) == 0) &&
                // but the reflected class is
                ((reflectionModifier & Modifier.ABSTRACT) != 0) &&
                // and it isn't an enum
                !isEnumType()) {
            // that is a problem
            return false;
        }
        // ABSTRACT check passed, so mask off ABSTRACT
        reflectionModifier &= ~Modifier.ABSTRACT;
        apiModifier &= ~Modifier.ABSTRACT;

        if (isAnnotation()) {
            reflectionModifier &= ~CLASS_MODIFIER_ANNOTATION;
        }
        if (mClass.isInterface()) {
            reflectionModifier &= ~(Modifier.INTERFACE);
        }
        if (isEnumType() && mClass.isEnum()) {
            reflectionModifier &= ~CLASS_MODIFIER_ENUM;
        }

        return ((reflectionModifier == apiModifier) &&
                (isEnumType() == mClass.isEnum()));
    }

    /**
     * Checks if the class under test is compliant with regards to
     * annnotations when compared to the API.
     *
     * @return true if the class is compliant
     */
    private boolean checkClassAnnotationCompliace() {
        if (mClass.isAnnotation()) {
            // check annotation
            for (String inter : implInterfaces) {
                if ("java.lang.annotation.Annotation".equals(inter)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Checks if the class under test extends the proper classes
     * according to the API.
     *
     * @return true if the class is compliant.
     */
    private boolean checkClassExtendsCompliance() {
        // Nothing to check if it doesn't extend anything.
        if (mExtendedClass != null) {
            Class<?> superClass = mClass.getSuperclass();
            if (superClass == null) {
                // API indicates superclass, reflection doesn't.
                return false;
            }

            if (superClass.getCanonicalName().equals(mExtendedClass)) {
                return true;
            }

            if (mAbsoluteClassName.equals("android.hardware.SensorManager")) {
                // FIXME: Please see Issue 1496822 for more information
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * Checks if the class under test implements the proper interfaces
     * according to the API.
     *
     * @return true if the class is compliant
     */
    private boolean checkClassImplementsCompliance() {
        Class<?>[] interfaces = mClass.getInterfaces();
        Set<String> interFaceSet = new HashSet<String>();

        for (Class<?> c : interfaces) {
            interFaceSet.add(c.getCanonicalName());
        }

        for (String inter : implInterfaces) {
            if (!interFaceSet.contains(inter)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks that the class found through reflection matches the
     * specification from the API xml file.
     */
    @SuppressWarnings("unchecked")
    private void checkClassCompliance() {
        try {
            mAbsoluteClassName = mPackageName + "." + mShortClassName;
            mClass = findMatchingClass();

            if (mClass == null) {
                // No class found, notify the observer according to the class type
                if (JDiffType.INTERFACE.equals(mClassType)) {
                    mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISSING_INTERFACE,
                            mAbsoluteClassName,
                            "Classloader is unable to find " + mAbsoluteClassName);
                } else {
                    mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISSING_CLASS,
                            mAbsoluteClassName,
                            "Classloader is unable to find " + mAbsoluteClassName);
                }

                return;
            }
            if (!checkClassModifiersCompliance()) {
                logMismatchInterfaceSignature(mAbsoluteClassName,
                        "Non-compatible class found when looking for " +
                        toSignatureString());
                return;
            }

            if (!checkClassAnnotationCompliace()) {
                logMismatchInterfaceSignature(mAbsoluteClassName,
                "Annotation mismatch");
                return;
            }

            if (!mClass.isAnnotation()) {
                // check father class
                if (!checkClassExtendsCompliance()) {
                    logMismatchInterfaceSignature(mAbsoluteClassName,
                    "Extends mismatch");
                    return;
                }

                // check implements interface
                if (!checkClassImplementsCompliance()) {
                    logMismatchInterfaceSignature(mAbsoluteClassName,
                    "Implements mismatch");
                    return;
                }
            }
        } catch (Exception e) {
            SignatureTestLog.e("Got exception when checking field compliance", e);
            mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.CAUGHT_EXCEPTION,
                    mAbsoluteClassName,
            "Exception!");
        }
    }


    /**
     * Convert the class into a printable signature string.
     *
     * @return the signature string
     */
    public String toSignatureString() {
        StringBuffer sb = new StringBuffer();

        String accessLevel = convertModifiersToAccessLevel(mModifier);
        if (!"".equals(accessLevel)) {
            sb.append(accessLevel).append(" ");
        }
        if (!JDiffType.INTERFACE.equals(mClassType)) {
            String modifierString = convertModifersToModifierString(mModifier);
            if (!"".equals(modifierString)) {
                sb.append(modifierString).append(" ");
            }
            sb.append("class ");
        } else {
            sb.append("interface ");
        }
        // class name
        sb.append(mShortClassName);

        // does it extends something?
        if (mExtendedClass != null) {
            sb.append(" extends ").append(mExtendedClass).append(" ");
        }

        // implements something?
        if (implInterfaces.size() > 0) {
            sb.append(" implements ");
            for (int x = 0; x < implInterfaces.size(); x++) {
                String interf = implInterfaces.get(x);
                sb.append(interf);
                // if not last elements
                if (x + 1 != implInterfaces.size()) {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }

    private void logMismatchInterfaceSignature(String classFullName, String errorMessage) {
        if (JDiffType.INTERFACE.equals(mClassType)) {
            mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISMATCH_INTERFACE,
                    classFullName,
                    errorMessage);
        } else {
            mResultObserver.notifyFailure(SignatureTestActivity.FAILURE_TYPE.MISMATCH_CLASS,
                    classFullName,
                    errorMessage);
        }
    }

    /**
     * Sees if the class under test is actually an enum.
     *
     * @return true if this class is enum
     */
    private boolean isEnumType() {
        return "java.lang.Enum".equals(mExtendedClass);
    }

    /**
     * Finds the reflected class for the class under test.
     *
     * @return the reflected class, or null if not found.
     */
    @SuppressWarnings("unchecked")
    private Class<?> findMatchingClass() {
        // even if there are no . in the string, split will return an
        // array of length 1
        String[] classNameParts = mShortClassName.split("\\.");
        String currentName = mPackageName + "." + classNameParts[0];

        try {
            // Check to see if the class we're looking for is the top
            // level class.
            Class<?> clz = Class.forName(currentName,
                    false,
                    this.getClass().getClassLoader());
            if (clz.getCanonicalName().equals(mAbsoluteClassName)) {
                return clz;
            }

            // Then it must be an inner class.
            for (int x = 1; x < classNameParts.length; x++) {
                clz = findInnerClassByName(clz, classNameParts[x]);
                if (clz == null) {
                    return null;
                }
                if (clz.getCanonicalName().equals(mAbsoluteClassName)) {
                    return clz;
                }
            }
        } catch (ClassNotFoundException e) {
            SignatureTestLog.e("ClassNotFoundException for " + mPackageName + "." + mShortClassName, e);
            return null;
        }
        return null;
    }

    /**
     * Searches the class for the specified inner class.
     *
     * @param clz the class to search in.
     * @param simpleName the simpleName of the class to find
     * @returns the class being searched for, or null if it can't be found.
     */
    private Class<?> findInnerClassByName(Class<?> clz, String simpleName) {
        for (Class<?> c : clz.getClasses()) {
            if (c.getSimpleName().equals(simpleName)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Sees if the class under test is actually an annotation.
     *
     * @return true if this class is Annotation.
     */
    private boolean isAnnotation() {
        if (implInterfaces.contains("java.lang.annotation.Annotation")) {
            return true;
        }
        return false;
    }

    /**
     * Gets the class name for the class under test.
     *
     * @return the class name.
     */
    public String getClassName() {
        return mShortClassName;
    }

    /**
     * Sets the modifier for the class under test.
     *
     * @param modifier the modifier
     */
    public void setModifier(int modifier) {
        mModifier = modifier;
    }

    /**
     * Sets the return type for the class under test.
     *
     * @param type the return type
     */
    public void setType(JDiffType type) {
        mClassType = type;
    }

    /**
     * Sets the class that is beign extended for the class under test.
     *
     * @param extendsClass the class being extended.
     */
    public void setExtendsClass(String extendsClass) {
        mExtendedClass = extendsClass;
    }

    /**
     * Registers a ResultObserver to process the output from the
     * compliance testing done in this class.
     *
     * @param resultObserver the observer to register.
     */
    public void registerResultObserver(ResultObserver resultObserver) {
        mResultObserver = resultObserver;
    }

    /**
     * Converts WildcardType array into a jdiff compatible string..
     * This is a helper function for typeToString.
     *
     * @param types array of types to format.
     * @return the jdiff formatted string.
     */
    private static String concatWildcardTypes(Type[] types) {
        StringBuffer sb = new StringBuffer();
        int elementNum = 0;
        for (Type t : types) {
            sb.append(typeToString(t));
            if (++elementNum < types.length) {
                sb.append(" & ");
            }
        }
        return sb.toString();
    }

    /**
     * Converts a Type into a jdiff compatible String.  The returned
     * types from this function should match the same Strings that
     * jdiff is providing to us.
     *
     * @param type the type to convert.
     * @return the jdiff formatted string.
     */
    private static String typeToString(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;

            StringBuffer sb = new StringBuffer();
            sb.append(typeToString(pt.getRawType()));
            sb.append("<");

            int elementNum = 0;
            Type[] types = pt.getActualTypeArguments();
            for (Type t : types) {
                sb.append(typeToString(t));
                if (++elementNum < types.length) {
                    sb.append(", ");
                }
            }

            sb.append(">");
            return sb.toString();
        } else if (type instanceof TypeVariable) {
            return ((TypeVariable<?>) type).getName();
        } else if (type instanceof Class) {
            return ((Class<?>) type).getCanonicalName();
        } else if (type instanceof GenericArrayType) {
            String typeName = typeToString(((GenericArrayType) type).getGenericComponentType());
            return typeName + "[]";
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType) type;
            Type[] lowerBounds = wt.getLowerBounds();
            if (lowerBounds.length == 0) {
                String name = "? extends " + concatWildcardTypes(wt.getUpperBounds());

                // Special case for ?
                if (name.equals("? extends java.lang.Object")) {
                    return "?";
                } else {
                    return name;
                }
            } else {
                String name = concatWildcardTypes(wt.getUpperBounds()) +
                " super " +
                concatWildcardTypes(wt.getLowerBounds());
                // Another special case for ?
                name = name.replace("java.lang.Object", "?");
                return name;
            }
        } else {
            throw new RuntimeException("Got an unknown java.lang.Type");
        }
    }

    /**
     * Cleans up jdiff parameters to canonicalize them.
     *
     * @param paramType the parameter from jdiff.
     * @return the scrubbed version of the parameter.
     */
    private static String scrubJdiffParamType(String paramType) {
        // <? extends java.lang.Object and <?> are the same, so
        // canonicalize them to one form.
        return paramType.replace("<? extends java.lang.Object>", "<?>");
    }
}
