package its.madruga.wpp.xposed;

import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.OpCodeMatchType;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.FieldMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.query.matchers.base.OpCodesMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.ClassDataList;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.result.MethodDataList;
import org.luckypray.dexkit.result.UsingFieldData;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import its.madruga.wpp.xposed.plugins.core.XMain;

public class Unobfuscator {

    private static DexKitBridge dexkit;

    public static final String BUBBLE_COLORS_BALLOON_INCOMING_NORMAL = "balloon_incoming_normal";
    public static final String BUBBLE_COLORS_BALLOON_INCOMING_NORMAL_EXT = "balloon_incoming_normal_ext";
    public static final String BUBBLE_COLORS_BALLOON_OUTGOING_NORMAL = "balloon_outgoing_normal";
    public static final String BUBBLE_COLORS_BALLOON_OUTGOING_NORMAL_EXT = "balloon_outgoing_normal_ext";

    private static final HashMap<String, Object> cache = new HashMap<>();

    static {
        System.loadLibrary("dexkit");
    }

    public static boolean initDexKit(String path) {
        try {
            dexkit = DexKitBridge.create(path);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    // TODO: Functions to find classes and methods
    public static Method findFirstMethodUsingStrings(ClassLoader classLoader, StringMatchType type, String... strings) {
        MethodMatcher matcher = new MethodMatcher();
        for (String string : strings) {
            matcher.addUsingString(string, type);
        }
        MethodDataList result = dexkit.findMethod(new FindMethod().matcher(matcher));
        if (result.isEmpty()) return null;
        try {
            return result.get(0).getMethodInstance(classLoader);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    public static Method[] findAllMethodUsingStrings(ClassLoader classLoader, StringMatchType type, String... strings) {
        MethodMatcher matcher = new MethodMatcher();
        for (String string : strings) {
            matcher.addUsingString(string, type);
        }
        MethodDataList result = dexkit.findMethod(new FindMethod().matcher(matcher));
        if (result.isEmpty()) return new Method[0];
        return result.stream().filter(MethodData::isMethod).map(methodData -> {
            try {
                return methodData.getMethodInstance(classLoader);
            } catch (NoSuchMethodException e) {
                return null;
            }
        }).filter(Objects::nonNull).toArray(Method[]::new);
    }

    public static Class<?> findFirstClassUsingStrings(ClassLoader classLoader, StringMatchType type, String... strings) throws Exception {
        var matcher = new ClassMatcher();
        for (String string : strings) {
            matcher.addUsingString(string, type);
        }
        var result = dexkit.findClass(new FindClass().matcher(matcher));
        if (result.isEmpty()) return null;
        return result.get(0).getInstance(classLoader);
    }

    public static Field getFieldByType(Class<?> cls, Class<?> type) {
        return Arrays.stream(cls.getDeclaredFields()).filter(f -> f.getType().equals(type)).findFirst().orElse(null);
    }

    public static Field getFieldByExtendType(Class<?> cls, Class<?> type) {
        return Arrays.stream(cls.getFields()).filter(f -> type.isAssignableFrom(f.getType())).findFirst().orElse(null);
    }

    public static String getMethodDescriptor(Method method) {
        return method.getDeclaringClass().getName() + "->" + method.getName() + "(" + Arrays.stream(method.getParameterTypes()).map(Class::getName).collect(Collectors.joining(",")) + ")";
    }

    public static String getFieldDescriptor(Field field) {
        return field.getDeclaringClass().getName() + "->" + field.getName() + ":" + field.getType().getName();
    }


    // TODO: Classes and Methods for FreezeSeen
    public static Method loadFreezeSeenMethod(ClassLoader classLoader) {
        return findFirstMethodUsingStrings(classLoader, StringMatchType.Contains, "presencestatemanager/setAvailable/new-state");
    }

    // TODO: Classes and Methods for GhostMode
    public static Method loadGhostModeMethod(ClassLoader classLoader) throws Exception {
        Method method = findFirstMethodUsingStrings(classLoader, StringMatchType.Contains, "HandleMeComposing/sendComposing");
        if (method == null) throw new Exception("GhostMode method not found");
        if (method.getParameterTypes().length > 2 && method.getParameterTypes()[2] == int.class)
            return method;
        throw new Exception("GhostMode method not found parameter type");
    }

    // TODO: Classes and Methods for Receipt

    public static Method loadReceiptMethod(ClassLoader classLoader) throws Exception {
        if (cache.containsKey("receipt")) return (Method) cache.get("receipt");
        Method[] methods = findAllMethodUsingStrings(classLoader, StringMatchType.Equals, "privacy_token", "false", "receipt");
        var deviceJidClass = XposedHelpers.findClass("com.whatsapp.jid.DeviceJid", classLoader);
        Method bestMethod = Arrays.stream(methods).filter(method -> method.getParameterTypes().length > 1 && method.getParameterTypes()[1] == deviceJidClass).findFirst().orElse(null);
        if (bestMethod == null) throw new Exception("Receipt method not found");
        cache.put("receipt", bestMethod);
        return bestMethod;
    }

    // TODO: Classes and Methods for HideForward

    public static Method loadForwardTagMethod(ClassLoader classLoader) throws Exception {
        Class<?> messageInfoClass = loadThreadMessageClass(classLoader);
        if (messageInfoClass == null) throw new Exception("MessageInfo class not found");
        Method result = Arrays.stream(messageInfoClass.getMethods()).filter(m ->
                m.getParameterTypes().length > 0 &&
                        m.getParameterTypes()[0] == int.class &&
                        m.getReturnType().equals(void.class) &&
                        Modifier.isPublic(m.getModifiers())).findFirst().orElse(null);
        if (result == null) throw new Exception("ForwardTag method not found");
        return result;
    }

    public static Class<?> loadForwardClassMethod(ClassLoader classLoader) throws Exception {
        return findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "UserActions/userActionForwardMessage");
    }


    // TODO: Classes and Methods for HideView
    public static Method loadHideViewOpenChatMethod(ClassLoader classLoader) throws Exception {
        Class<?> receiptsClass = loadReadReceiptsClass(classLoader);
        Method method = Arrays.stream(receiptsClass.getMethods()).filter(m -> m.getParameterTypes().length > 0 && m.getParameterTypes()[0].equals(Collection.class) && m.getReturnType().equals(HashMap.class)).findFirst().orElse(null);
        if (method == null) throw new Exception("HideViewOpenChat method not found");
        return method;
    }

    public static Method loadHideViewInChatMethod(ClassLoader classLoader) throws Exception {
        Method method = findFirstMethodUsingStrings(classLoader, StringMatchType.Contains, "ReadReceipts/PrivacyTokenDecisionNotComputed");
        if (method == null) throw new Exception("HideViewInChat method not found");
        return method;
    }

    public static Method loadHideViewAudioMethod(ClassLoader loader) throws Exception {
        var result = findFirstMethodUsingStrings(loader, StringMatchType.Contains, "MessageStatusStore/update/nosuchmessage");
        if (result == null) throw new Exception("HideViewAudio method not found");
        return result;
    }

    public static Method loadHideOnceViewMethod(ClassLoader loader) throws Exception {
        var result = findFirstMethodUsingStrings(loader, StringMatchType.Contains, "presencestatemanager/setAvailable/new-state:");
        if (result == null) throw new Exception("HideViewAudio method not found");
        return result;
    }

    public static Class<?> loadReadReceiptsClass(ClassLoader classLoader) throws Exception {
        return findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "acknowledgeMessageSilent");
    }

    public static Method loadHideViewJidMethod(ClassLoader classLoader) throws Exception {
        Class<?> messageInfoClass = loadThreadMessageClass(classLoader);
        var called = dexkit.findMethod(new FindMethod().matcher(new MethodMatcher().addUsingString("statusmanager/markstatusasseen/sending status"))).get(0);
        var result = dexkit.findMethod(new FindMethod().matcher(new MethodMatcher().paramCount(1)
                .addParamType(messageInfoClass.getName()).returnType(void.class)
                .modifiers(Modifier.PUBLIC).addCall(called.getDescriptor())));
        if (result.isEmpty()) throw new Exception("HideViewJid method not found");
        return result.get(0).getMethodInstance(classLoader);
    }

    public static Class<?> loadThreadMessageClass(ClassLoader classLoader) throws Exception {
        if (cache.containsKey("message")) return (Class<?>) cache.get("message");
        var messageClass = findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "FMessage/getSenderUserJid/key.id");
        if (messageClass == null) throw new Exception("Message class not found");
        cache.put("message", messageClass);
        return messageClass;
    }

    // TODO: Classes and Methods for BubbleColors
    private static ClassDataList loadBubbleColorsClass() throws Exception {
        if (cache.containsKey("balloon")) return (ClassDataList) cache.get("balloon");
        var balloonIncomingNormal = XMain.mApp.getResources().getIdentifier(BUBBLE_COLORS_BALLOON_INCOMING_NORMAL, "drawable", XMain.mApp.getPackageName());
        var balloonIncomingNormalExt = XMain.mApp.getResources().getIdentifier(BUBBLE_COLORS_BALLOON_INCOMING_NORMAL_EXT, "drawable", XMain.mApp.getPackageName());
        var balloonOutgoingNormal = XMain.mApp.getResources().getIdentifier(BUBBLE_COLORS_BALLOON_OUTGOING_NORMAL, "drawable", XMain.mApp.getPackageName());
        var balloonOutgoingNormalExt = XMain.mApp.getResources().getIdentifier(BUBBLE_COLORS_BALLOON_OUTGOING_NORMAL_EXT, "drawable", XMain.mApp.getPackageName());
        var clsBubbleColors = dexkit.findClass(new FindClass().matcher(new ClassMatcher()
                .addMethod(new MethodMatcher().addUsingNumber(balloonIncomingNormal))
                .addMethod(new MethodMatcher().addUsingNumber(balloonOutgoingNormal))
                .addMethod(new MethodMatcher().addUsingNumber(balloonIncomingNormalExt))
                .addMethod(new MethodMatcher().addUsingNumber(balloonOutgoingNormalExt))
        ));
        cache.put("balloon", clsBubbleColors);
        return clsBubbleColors;
    }

    public static Method loadBubbleColorsMethod(ClassLoader classLoader, String name) throws Exception {
        var clsBubbleColors = loadBubbleColorsClass();
        var id = XMain.mApp.getResources().getIdentifier(name, "drawable", XMain.mApp.getPackageName());
        var result = dexkit.findMethod(new FindMethod().searchInClass(clsBubbleColors).matcher(new MethodMatcher().addUsingNumber(id)));
        return result.get(0).getMethodInstance(classLoader);
    }


    // TODO: Classes and Methods for XChatFilter

    public static Method loadTabListMethod(ClassLoader classLoader) throws Exception {
        Class<?> classMain = findFirstClassUsingStrings(classLoader, StringMatchType.Equals, "mainContainer");
        if (classMain == null) throw new Exception("mainContainer class not found");
        Method method = Arrays.stream(classMain.getMethods()).filter(m -> m.getName().equals("onCreate")).findFirst().orElse(null);
        if (method == null) throw new Exception("onCreate method not found");
        return method;
    }

    public static Method loadGetTabMethod(ClassLoader classLoader) throws Exception {
        if (cache.containsKey("tab")) return (Method) cache.get("tab");
        Method result = findFirstMethodUsingStrings(classLoader, StringMatchType.Contains, "Invalid tab id: 600");
        if (result == null) throw new Exception("GetTab method not found");
        cache.put("tab", result);
        return result;
    }

    public static Method loadTabFragmentMethod(ClassLoader classLoader) throws Exception {
        Class<?> clsFrag = XposedHelpers.findClass("com.whatsapp.conversationslist.ConversationsFragment", classLoader);
        Method result = Arrays.stream(clsFrag.getDeclaredMethods()).filter(m -> m.getParameterTypes().length == 0 && m.getReturnType().equals(List.class)).findFirst().orElse(null);
        if (result == null) throw new Exception("TabFragment method not found");
        return result;
    }

    public static Method loadTabNameMethod(ClassLoader classLoader) throws Exception {
        Method tabListMethod = loadGetTabMethod(classLoader);
        Class<?> cls = tabListMethod.getDeclaringClass();
        if (Modifier.isAbstract(cls.getModifiers())) {
            var findClass = dexkit.findClass(new FindClass().matcher(new ClassMatcher().superClass(cls.getName()).addUsingString("The item position should be less")));
            cls = findClass.get(0).getInstance(classLoader);
        }
        Method result = Arrays.stream(cls.getMethods()).filter(m -> m.getParameterTypes().length == 1 && m.getReturnType().equals(String.class)).findFirst().orElse(null);
        if (result == null) throw new Exception("TabName method not found");
        return result;
    }

    public static Method loadFabMethod(ClassLoader classLoader) throws Exception {
        Class<?> cls = XposedHelpers.findClass("com.whatsapp.conversationslist.ConversationsFragment", classLoader);
        List<ClassData> classes = List.of(dexkit.getClassData(cls));
        var result = dexkit.findMethod(new FindMethod().searchInClass(classes).matcher(new MethodMatcher().paramCount(0).usingNumbers(200).returnType(int.class)));
        if (result.isEmpty()) throw new Exception("Fab method not found");
        return result.get(0).getMethodInstance(classLoader);
    }

    public static Method loadIconTabMethod(ClassLoader classLoader) throws Exception {
        if (cache.containsKey("iconTab")) return (Method) cache.get("iconTab");
        Method result = findFirstMethodUsingStrings(classLoader, StringMatchType.Contains, "homeFabManager");
        if (result == null) throw new Exception("IconTab method not found");
        cache.put("iconTab", result);
        return result;
    }

    public static Field loadIconTabField(ClassLoader classLoader) throws Exception {
        if (cache.containsKey("iconTabField")) return (Field) cache.get("iconTabField");
        Class<?> cls = loadIconTabMethod(classLoader).getDeclaringClass();
        Class<?> clsType = findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "Tried to set badge");
        var result = Arrays.stream(cls.getFields()).filter(f -> f.getType().equals(clsType)).findFirst().orElse(null);
        if (result == null) throw new Exception("IconTabField not found");
        cache.put("iconTabField", result);
        return result;
    }

    public static Field loadIconTabLayoutField(ClassLoader classLoader) throws Exception {
        if (cache.containsKey("iconTabLayoutField")) return (Field) cache.get("iconTabLayoutField");
        Class<?> clsType = loadIconTabField(classLoader).getType();
        Class<?> framelayout = findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "android:menu:presenters");
        var result = Arrays.stream(clsType.getFields()).filter(f -> f.getType().equals(framelayout)).findFirst().orElse(null);
        if (result == null) throw new Exception("IconTabLayoutField not found");
        cache.put("iconTabLayoutField", result);
        return result;
    }

    public static Field loadIconMenuField(ClassLoader classLoader) throws Exception {
        Class<?> clsType = loadIconTabLayoutField(classLoader).getType();
        Class<?> menuClass = findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "Maximum number of items");
        return Arrays.stream(clsType.getFields()).filter(f -> f.getType().equals(menuClass)).findFirst().orElse(null);
    }

    public static Method loadTabCountMethod(ClassLoader classLoader) throws Exception {
        Method result = findFirstMethodUsingStrings(classLoader, StringMatchType.Contains, "required free space should be > 0");
        if (result == null) throw new Exception("TabCount method not found");
        return result;
    }

    public static Field loadTabCountField(ClassLoader classLoader) throws Exception {
        Class<?> homeActivity = XposedHelpers.findClass("com.whatsapp.HomeActivity", classLoader);
        Class<?> pager = loadGetTabMethod(classLoader).getDeclaringClass();
        return getFieldByType(homeActivity, pager);
    }

    public static Method loadEnableCountTabMethod(ClassLoader classLoader) throws Exception {
        var result = findFirstMethodUsingStrings(classLoader, StringMatchType.Contains, "Tried to set badge for invalid");
        if (result == null) throw new Exception("EnableCountTab method not found");
        return result;
    }

    // TODO: Classes and methods to TimeToSeconds

    public static Method loadTimeToSecondsMethod(ClassLoader classLoader) throws Exception {
        Class<?> cls = findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "aBhHKm");
        if (cls == null) throw new Exception("TimeToSeconds class not found");
        Method result = Arrays.stream(cls.getMethods()).filter(
                m -> m.getParameterTypes().length == 2 &&
                        m.getParameterTypes()[1].equals(long.class) &&
                        m.getReturnType().equals(String.class)
        ).findFirst().orElse(null);
        if (result == null) throw new Exception("TimeToSeconds method not found");
        return result;
    }

    // TODO: Classes and methods to DndMode

    public static Method loadDndModeMethod(ClassLoader classLoader) throws Exception {
        var method = findFirstMethodUsingStrings(classLoader, StringMatchType.Equals, "MessageHandler/start");
        if (method == null) throw new Exception("DndMode method not found");
        return method;
    }

    // TODO: Classes and methods to MediaQuality

    private static Class<?> loadMediaQualityClass(ClassLoader classLoader) throws Exception {
        if (cache.containsKey("MediaQuality")) return (Class<?>) cache.get("MediaQuality");
        var clazzMediaClass = findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "getCorrectedResolution");
        if (clazzMediaClass == null) throw new Exception("MediaQuality class not found");
        cache.put("MediaQuality", clazzMediaClass);
        return clazzMediaClass;
    }

    public static Method loadMediaQualityResolutionMethod(ClassLoader classLoader) throws Exception {
        var clazz = loadMediaQualityClass(classLoader);
        return Arrays.stream(clazz.getDeclaredMethods()).filter(
                m -> m.getParameterTypes().length == 3 &&
                        m.getParameterTypes()[0].equals(int.class) &&
                        m.getParameterTypes()[1].equals(int.class) &&
                        m.getParameterTypes()[2].equals(int.class)
        ).findFirst().orElse(null);
    }

    public static Method loadMediaQualityBitrateMethod(ClassLoader classLoader) throws Exception {
        var clazz = loadMediaQualityClass(classLoader);
        return Arrays.stream(clazz.getDeclaredMethods()).filter(
                m -> m.getParameterTypes().length == 1 &&
                        m.getParameterTypes()[0].equals(int.class) &&
                        m.getReturnType().equals(int.class)
        ).findFirst().orElse(null);
    }

    public static Method loadMediaQualityVideoMethod(ClassLoader classLoader) throws Exception {
        var clazz = loadMediaQualityClass(classLoader);
        return Arrays.stream(clazz.getDeclaredMethods()).filter(
                method1 -> method1.getParameterTypes().length == 3 &&
                        method1.getParameterTypes()[2].equals(int.class)
                        && method1.getReturnType().equals(Pair.class)
        ).findFirst().orElse(null);
    }

    public static Method loadMediaQualityImageMethod(ClassLoader classLoader) throws Exception {
        var method = findFirstMethodUsingStrings(classLoader, StringMatchType.Contains, "Unknown IntField");
        if (method == null) throw new Exception("MediaQualityImage method not found");
        return method;
    }

    // TODO: Classes and methods to ShareLimit

    private static MethodData loadShareLimitMethodData() throws Exception {
        var methods = dexkit.findMethod(new FindMethod().matcher(new MethodMatcher()
                .addUsingString("send_max_video_duration")));
        if (methods.isEmpty()) throw new Exception("ShareLimit method not found");
        return methods.get(0);
    }

    public static Method loadShareLimitMethod(ClassLoader classLoader) throws Exception {
        return loadShareLimitMethodData().getMethodInstance(classLoader);
    }

    public static Field loadShareLimitField(ClassLoader classLoader) throws Exception {
        var methodData = loadShareLimitMethodData();
        var clazz = methodData.getMethodInstance(classLoader).getDeclaringClass();
        var fields = methodData.getUsingFields();
        for (UsingFieldData field : fields) {
            Field field1 = field.getField().getFieldInstance(classLoader);
            if (field1.getType() == boolean.class && field1.getDeclaringClass() == clazz) {
                return field1;
            }
        }
        throw new Exception("ShareLimit field not found");
    }

    // TODO: Classes and methods to StatusDownload

    public static Method loadStatusActivePage(ClassLoader classLoader) throws Exception {
        var method = findFirstMethodUsingStrings(classLoader, StringMatchType.Contains, "playbackFragment/setPageActive");
        if (method == null) throw new Exception("StatusActivePage method not found");
        return method;
    }

    public static Class<?> loadStatusDownloadMediaClass(ClassLoader classLoader) throws Exception {
        if (cache.containsKey("StatusDownloadMedia"))
            return (Class<?>) cache.get("StatusDownloadMedia");
        var clazz = findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "FMessageVideo/Cloned");
        if (clazz == null) throw new Exception("StatusDownloadMedia class not found");
        cache.put("StatusDownloadMedia", clazz);
        return clazz;
    }

    public static Class loadMenuStatusClass(ClassLoader loader) throws Exception {
        var clazz = findFirstClassUsingStrings(loader, StringMatchType.Contains, "chatSettingsStore", "post_status_in_companion", "systemFeatures");
        if (clazz == null) throw new Exception("MenuStatus class not found");
        return clazz;
    }

    public static Field loadStatusDownloadFileField(ClassLoader classLoader) throws Exception {
        if (cache.containsKey("StatusDownloadFile")) return (Field) cache.get("StatusDownloadFile");
        var clazz = loadStatusDownloadMediaClass(classLoader);
        var clazz2 = clazz.getField("A01").getType();
        var field = getFieldByType(clazz2, File.class);
        if (field == null) throw new Exception("StatusDownloadFile field not found");
        cache.put("StatusDownloadFile", field);
        return field;
    }

    public static Class<?> loadStatusDownloadSubMenuClass(ClassLoader classLoader) throws Exception {
        var classes = dexkit.findClass(
                new FindClass().matcher(
                        new ClassMatcher().addMethod(
                                new MethodMatcher()
                                        .addUsingString("MenuPopupHelper", StringMatchType.Contains)
                                        .returnType(void.class)
                        )
                )
        );
        if (classes.isEmpty()) throw new Exception("StatusDownloadSubMenu method not found");
        return classes.get(0).getInstance(classLoader);
    }

    public static Class<?> loadStatusDownloadMenuClass(ClassLoader classLoader) throws Exception {
        var clazz = findFirstClassUsingStrings(classLoader, StringMatchType.Contains, "android:menu:expandedactionview");
        if (clazz == null) throw new Exception("StatusDownloadMenu class not found");
        return clazz;
    }

    // TODO: Classes and methods to ViewOnce

    public static Method[] loadViewOnceMethod(ClassLoader classLoader) throws Exception {
        var method = dexkit.findMethod(new FindMethod().matcher(new MethodMatcher().addUsingString("SELECT state FROM message_view_once_media", StringMatchType.Contains)));
        if (method.isEmpty()) throw new Exception("ViewOnce method not found");
        var methodData = method.get(0);
        var listMethods = methodData.getInvokes();
        var list = new ArrayList<Method>();
        for (MethodData m : listMethods) {
            var mInstance = m.getMethodInstance(classLoader);
            if (mInstance.getDeclaringClass().isInterface() && mInstance.getDeclaringClass().getMethods().length == 2) {
                ClassDataList listClasses = dexkit.findClass(new FindClass().matcher(new ClassMatcher().addInterface(mInstance.getDeclaringClass().getName())));
                for (ClassData c : listClasses) {
                    Class<?> clazz = c.getInstance(classLoader);
                    Method m1 = clazz.getMethod(mInstance.getName(), mInstance.getParameterTypes());
                    list.add(m1);
                }
                return list.toArray(new Method[0]);
            }
        }
        throw new Exception("ViewOnce method not found");

    }

    public static Method loadViewOnceDownloadMenuMethod(ClassLoader classLoader) throws Exception {
        if (cache.containsKey("ViewOnceDownloadMenu"))
            return (Method) cache.get("ViewOnceDownloadMenu");
        var clazz = XposedHelpers.findClass("com.whatsapp.mediaview.MediaViewFragment", classLoader);
        var method = Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.getParameterCount() == 2 &&
                Objects.equals(m.getParameterTypes()[0], Menu.class) &&
                Objects.equals(m.getParameterTypes()[1], MenuInflater.class) &&
                m.getDeclaringClass() == clazz
        ).findFirst();
        if (!method.isPresent()) throw new Exception("ViewOnceDownloadMenu method not found");
        cache.put("ViewOnceDownloadMenu", method.get());
        return method.get();
    }

    public static Field loadViewOnceDownloadMenuField(ClassLoader classLoader) throws Exception {
        var method = loadViewOnceDownloadMenuMethod(classLoader);
        var clazz = XposedHelpers.findClass("com.whatsapp.mediaview.MediaViewFragment", classLoader);
        var methodData = dexkit.findMethod(new FindMethod().matcher(new MethodMatcher().declaredClass(clazz).name(method.getName()))).get(0);
        XposedBridge.log(methodData.getDescriptor());
        var fields = methodData.getUsingFields();
        for (UsingFieldData field : fields) {
            Field field1 = field.getField().getFieldInstance(classLoader);
            if (field1.getType() == int.class && field1.getDeclaringClass() == clazz) {
                return field1;
            }
        }
        throw new Exception("ViewOnceDownloadMenu field not found");
    }

    public static Field loadViewOnceDownloadMenuField2(ClassLoader classLoader) throws Exception {
        var methodData = dexkit.findMethod(new FindMethod().matcher(new MethodMatcher().addUsingString("photo_progress_fragment"))).get(0);
        var clazz = methodData.getMethodInstance(classLoader).getDeclaringClass();
        var fields = methodData.getUsingFields();
        for (UsingFieldData field : fields) {
            Field field1 = field.getField().getFieldInstance(classLoader);
            if (field1.getType() == int.class && field1.getDeclaringClass() == clazz) {
                return field1;
            }
        }
        throw new Exception("ViewOnceDownloadMenu field 2 not found");
    }

    public static Method loadViewOnceDownloadMenuCallMethod(ClassLoader classLoader) throws Exception {
        var clazz = XposedHelpers.findClass("com.whatsapp.mediaview.MediaViewFragment", classLoader);
        var method = Arrays.stream(clazz.getDeclaredMethods()).filter(m ->
                ((m.getParameterCount() == 2 && Objects.equals(m.getParameterTypes()[1], int.class) && Objects.equals(m.getParameterTypes()[0], clazz))
                        || (m.getParameterCount() == 1 && Objects.equals(m.getParameterTypes()[0], int.class))) &&
                        Modifier.isPublic(m.getModifiers()) && Object.class.isAssignableFrom(m.getReturnType())
        ).findFirst();
        if (!method.isPresent()) throw new Exception("ViewOnceDownloadMenuCall method not found");
        return method.get();
    }

    public static Class<?> loadExpandableWidgetClass(ClassLoader loader) throws Exception {
        var clazz = findFirstClassUsingStrings(loader, StringMatchType.Contains, "expandableWidgetHelper");
        if (clazz == null) throw new Exception("ExpandableWidgetHelper class not found");
        return clazz;
    }

    public static Class<?> loadMaterialShapeDrawableClass(ClassLoader loader) throws Exception {
        var clazz = findFirstClassUsingStrings(loader, StringMatchType.Contains, "Compatibility shadow requested");
        if (clazz == null) throw new Exception("MaterialShapeDrawable class not found");
        return clazz;
    }

    public static Class<?> loadCustomDrawableClass(ClassLoader loader) throws Exception {
        var clazz = findFirstClassUsingStrings(loader, StringMatchType.Contains, "closeIconEnabled");
        if (clazz == null) throw new Exception("CustomDrawable class not found");
        return clazz;
    }

    public static Method loadDeprecatedMethod(ClassLoader loader) throws Exception {
        var methods = findAllMethodUsingStrings(loader, StringMatchType.Contains, "software_forced_expiration");
        if (methods == null || methods.length == 0)
            throw new Exception("Deprecated method not found");
        var result = Arrays.stream(methods).filter(method -> method.getReturnType().equals(Date.class)).findFirst().orElse(null);
        if (result == null) throw new Exception("Deprecated method not found");
        return result;
    }

    public static Method loadPropsMethod(ClassLoader loader) throws Exception {
        var method = findFirstMethodUsingStrings(loader, StringMatchType.Contains, "Unknown BooleanField");
        if (method == null) throw new Exception("Props method not found");
        return method;
    }

    private static ClassData loadAntiRevokeImplClass(ClassLoader loader) throws Exception {
        var classes = dexkit.findClass(new FindClass().matcher(new ClassMatcher().addUsingString("smb_eu_tos_update_url")));
        if (classes.isEmpty()) throw new Exception("AntiRevokeImpl class not found");
        return classes.get(0);
    }

    public static Method loadAntiRevokeOnStartMethod(ClassLoader loader) throws Exception {
        Class<?> conversation = XposedHelpers.findClass("com.whatsapp.Conversation", loader);
        var classData = loadAntiRevokeImplClass(loader);
        MethodDataList mdOnStart = dexkit.findMethod(
                FindMethod.create().searchInClass(List.of(dexkit.getClassData(conversation)))
                        .matcher(MethodMatcher.create().addInvoke(Objects.requireNonNull(classData).getDescriptor() + "->onStart()V"))
        );
        if (mdOnStart.isEmpty()) throw new Exception("AntiRevokeOnStart method not found");
        return mdOnStart.get(0).getMethodInstance(loader);
    }

    public static Method loadAntiRevokeOnResumeMethod(ClassLoader loader) throws Exception {
        Class<?> conversation = XposedHelpers.findClass("com.whatsapp.Conversation", loader);
        var classData = loadAntiRevokeImplClass(loader);
        MethodDataList mdOnStart = dexkit.findMethod(
                FindMethod.create().searchInClass(List.of(dexkit.getClassData(conversation)))
                        .matcher(MethodMatcher.create().addInvoke(Objects.requireNonNull(classData).getDescriptor() + "->onResume()V"))
        );
        if (mdOnStart.isEmpty()) throw new Exception("AntiRevokeOnStart method not found");
        return mdOnStart.get(0).getMethodInstance(loader);
    }

    public static Field loadAntiRevokeConvChatField(ClassLoader loader) throws Exception {
        Class<?> chatClass = findFirstClassUsingStrings(loader, StringMatchType.Contains, "payment_chat_composer_entry_nux_shown");
        Class<?> conversation = XposedHelpers.findClass("com.whatsapp.Conversation", loader);
        Field field = getFieldByType(conversation, chatClass);
        if (field == null) throw new Exception("AntiRevokeConvChat field not found");
        return field;
    }

    public static Field loadAntiRevokeChatJidField(ClassLoader loader) throws Exception {
        if (cache.containsKey("loadAntiRevokeChatJidField"))
            return (Field) cache.get("loadAntiRevokeChatJidField");
        Class<?> chatClass = findFirstClassUsingStrings(loader, StringMatchType.Contains, "payment_chat_composer_entry_nux_shown");
        Class<?> jidClass = XposedHelpers.findClass("com.whatsapp.jid.Jid", loader);
        Field field = getFieldByExtendType(chatClass, jidClass);
        if (field == null) throw new Exception("AntiRevokeChatJid field not found");
        cache.put("loadAntiRevokeChatJidField", field);
        return field;
    }

    public static Method loadAntiRevokeMessageMethod(ClassLoader loader) throws Exception {
        Method method = findFirstMethodUsingStrings(loader, StringMatchType.Contains, "msgstore/edit/revoke");
        if (method == null) throw new Exception("AntiRevokeMessage method not found");
        return method;
    }

    public static Field loadAntiRevokeMessageKeyField(ClassLoader loader) throws Exception {
        Class<?> classExtendJid = loadAntiRevokeChatJidField(loader).getType();
        ClassDataList classes = dexkit.findClass(
                new FindClass().matcher(
                        new ClassMatcher().
                                addUsingString("remote_jid=")
                                .addField(new FieldMatcher().type(classExtendJid))
                )
        );
        if (classes.isEmpty()) throw new Exception("AntiRevokeMessageKey class not found");
        Class<?> messageKey = classes.get(0).getInstance(loader);
        var classMessage = loadThreadMessageClass(loader);
        var field = Arrays.stream(classMessage.getFields()).filter(f -> f.getType() == messageKey && Modifier.isFinal(f.getModifiers())).findFirst().orElse(null);
        if (field == null) throw new Exception("AntiRevokeMessageKey field not found");
        return field;
    }

    public static Method loadAntiRevokeBubbleMethod(ClassLoader loader) throws Exception {
        Class<?> bubbleClass = findFirstClassUsingStrings(loader, StringMatchType.Contains, "ConversationRow/setUpUserNameInGroupView");
        if (bubbleClass == null) throw new Exception("AntiRevokeBubble method not found");
        var result = Arrays.stream(bubbleClass.getMethods()).filter(m -> m.getParameterCount() > 1 && m.getParameterTypes()[0] == ViewGroup.class && m.getParameterTypes()[1] == TextView.class).findFirst().orElse(null);
        if (result == null) throw new Exception("AntiRevokeBubble method not found");
        return result;
    }

    public static Method loadUnknownStatusPlaybackMethod(ClassLoader loader) throws Exception {
        var statusPlaybackClass = XposedHelpers.findClass("com.whatsapp.status.playback.fragment.StatusPlaybackContactFragment", loader);
        var classData = List.of(dexkit.getClassData(statusPlaybackClass));
        var result = dexkit.findMethod(new FindMethod().
                searchInClass(classData).
                matcher(new MethodMatcher()
                        .addUsingString("xFamilyGating").
                        addUsingString("xFamilyCrosspostManager")));
        if (result.isEmpty()) throw new Exception("UnknownStatusPlayback method not found");
        return result.get(0).getMethodInstance(loader);
    }

    public static Field loadStatusPlaybackViewField(ClassLoader loader) throws Exception {
        Class<?> class1 = XposedHelpers.findClass("com.whatsapp.status.playback.widget.StatusPlaybackProgressView", loader);
        ClassDataList classView = dexkit.findClass(FindClass.create().matcher(
                ClassMatcher.create().methodCount(1).addFieldForType(class1)
        ));
        if (classView.isEmpty()) throw new Exception("StatusPlaybackView field not found");
        Class<?> clsViewStatus = classView.get(0).getInstance(loader);
        Class<?> class2 = XposedHelpers.findClass("com.whatsapp.status.playback.fragment.StatusPlaybackBaseFragment", loader);
        return Arrays.stream(class2.getDeclaredFields()).filter(f -> f.getType() == clsViewStatus).findFirst().orElse(null);
    }

    public static Class<?> loadMessageStoreClass(ClassLoader loader) throws Exception {
        var result = findFirstClassUsingStrings(loader, StringMatchType.Contains, "databasehelper/createDatabaseTables");
        if (result == null) throw new Exception("MessageStore class not found");
        return result;
    }

    public static Class<?> loadAxolotlClass(ClassLoader loader) throws Exception {
        var result = findFirstClassUsingStrings(loader, StringMatchType.Contains, "failed to open axolotl store");
        if (result == null) throw new Exception("Axolotl class not found");
        return result;
    }

    public static Method loadBlueOnReplayMessageJobMethod(ClassLoader loader) throws Exception {
        var result = findFirstMethodUsingStrings(loader, StringMatchType.Contains, "SendE2EMessageJob/e2e message send job added");
        if (result == null) throw new Exception("BlueOnReplayMessageJob method not found");
        return result;
    }

    public static Method loadBlueOnReplayWaJobManagerMethod(ClassLoader loader) throws Exception {
        var result = findFirstClassUsingStrings(loader, StringMatchType.Contains, "WaJobManager/start");
        var job = XposedHelpers.findClass("org.whispersystems.jobqueue.Job", loader);
        if (result == null) throw new Exception("BlueOnReplayWaJobManager method not found");
        var method = Arrays.stream(result.getMethods()).filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0] == job).findFirst().orElse(null);
        if (method == null) throw new Exception("BlueOnReplayWaJobManager method not found");
        return method;
    }

    public static Method loadArchiveHideViewMethod(ClassLoader loader) throws Exception {
        if (cache.containsKey("ArchiveHideView")) return (Method) cache.get("ArchiveHideView");
        var methods = findAllMethodUsingStrings(loader, StringMatchType.Contains, "archive/set-content-indicator-to-empty");
        if (methods.length == 0) throw new Exception("ArchiveHideView method not found");
        var clazz = methods[methods.length - 1].getDeclaringClass();
        var method = clazz.getMethod("setVisibility", boolean.class);
        cache.put("ArchiveHideView", method);
        return method;
    }

    public static Method loadArchiveOnclickCaptureMethod(ClassLoader loader) throws Exception {
        var clazz = loadArchiveHideViewMethod(loader).getDeclaringClass();
        return clazz.getMethod("setOnClickListener", View.OnClickListener.class);
    }

    public static Method loadAntiRevokeOnCallReceivedMethod(ClassLoader loader) throws Exception {
        var method = findFirstMethodUsingStrings(loader, StringMatchType.Contains, "VoiceService:callStateChangedOnUiThread");
        if (method == null) throw new Exception("OnCallReceiver method not found");
        return method;
    }

    public static Method loadAntiRevokeCallEndMethod(ClassLoader loader) throws Exception {
        var method = findFirstMethodUsingStrings(loader, StringMatchType.Contains, "voicefgservice/stop-service");
        if (method == null) throw new Exception("CallEndReceiver method not found");
        return method;
    }

    public static Field loadContactManagerField(ClassLoader loader) throws Exception {
        Class<?> class1 = findFirstClassUsingStrings(loader, StringMatchType.Contains, "contactmanager/permission problem:");
        if (class1 == null) throw new Exception("ContactManager field not found");
        Class HomeActivity = XposedHelpers.findClass("com.whatsapp.HomeActivity", loader);
        return getFieldByType(HomeActivity, class1);
    }

    public static Method loadGetContactInfoMethod(ClassLoader loader) throws Exception {
        Class<?> class1 = findFirstClassUsingStrings(loader, StringMatchType.Contains, "contactmanager/permission problem:");
        if (class1 == null) throw new Exception("GetContactInfo method not found");
        var methods = class1.getMethods();
        for (int i = 0; i < methods.length; i++) {
            var method = methods[i];
            if (method.getParameterCount() == 2 && method.getParameterTypes()[1] == boolean.class) {
                if (methods[i-1].getParameterCount() == 1)
                    return methods[i-1];
            }
        }
        throw new Exception("GetContactInfo 2 method not found");
    }

}
