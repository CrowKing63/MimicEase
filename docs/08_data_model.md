> **[MimicEase 사양서 — 08/11]** 독립 작업 가능 단위
> **프로젝트**: Google Project GameFace(Android) 기반 표정 인식 안드로이드 접근성 앱
> **스택**: Kotlin + Jetpack Compose, API 29+, MediaPipe 온디바이스 ML
> **전체 목차**: [`docs/00_INDEX.md`](./00_INDEX.md)

---

# 08. 데이터 모델 (Data Model)

## 8.1 Room Database — Entity

### 8.1.1 ProfileEntity

```kotlin
// data/local/entity/ProfileEntity.kt
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,                          // UUID.randomUUID().toString()
    val name: String,
    val icon: String,                        // 이모지 문자 또는 Material Icon 이름
    val isActive: Boolean,
    val sensitivity: Float = 1.0f,           // 0.5 ~ 2.0
    val globalCooldownMs: Int = 300,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 8.1.2 TriggerEntity

```kotlin
// data/local/entity/TriggerEntity.kt
@Entity(
    tableName = "triggers",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE   // 프로필 삭제 시 트리거도 삭제
        )
    ],
    indices = [Index("profileId")]
)
data class TriggerEntity(
    @PrimaryKey
    val id: String,                          // UUID
    val profileId: String,                   // FK → ProfileEntity.id
    val name: String,
    val blendShape: String,                  // 블렌드쉐이프 ID (예: "eyeBlinkRight")
    val threshold: Float,                    // 0.0 ~ 1.0
    val holdDurationMs: Int = 200,
    val cooldownMs: Int = 1000,
    val actionType: String,                  // Action sealed class의 타입 이름
    val actionParams: String = "{}",         // JSON 직렬화된 파라미터
    val isEnabled: Boolean = true,
    val priority: Int = 100,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### 8.1.3 ProfileWithTriggers (Room Relation)

```kotlin
// data/local/entity/ProfileWithTriggers.kt
data class ProfileWithTriggers(
    @Embedded val profile: ProfileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "profileId"
    )
    val triggers: List<TriggerEntity>
)
```

---

## 8.2 DAO (Data Access Object)

### 8.2.1 ProfileDao

```kotlin
@Dao
interface ProfileDao {
    @Transaction
    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun getAllProfilesWithTriggers(): Flow<List<ProfileWithTriggers>>

    @Transaction
    @Query("SELECT * FROM profiles WHERE isActive = 1 LIMIT 1")
    fun getActiveProfileWithTriggers(): Flow<ProfileWithTriggers?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("UPDATE profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE profiles SET isActive = 1 WHERE id = :id")
    suspend fun activate(id: String)
}
```

### 8.2.2 TriggerDao

```kotlin
@Dao
interface TriggerDao {
    @Query("SELECT * FROM triggers WHERE profileId = :profileId ORDER BY priority ASC")
    fun getTriggersByProfile(profileId: String): Flow<List<TriggerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trigger: TriggerEntity)

    @Update
    suspend fun update(trigger: TriggerEntity)

    @Delete
    suspend fun delete(trigger: TriggerEntity)

    @Query("UPDATE triggers SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM triggers WHERE profileId = :profileId")
    suspend fun deleteByProfile(profileId: String)
}
```

---

## 8.3 Domain 모델

Clean Architecture에서 Data Entity와 분리된 순수 Kotlin 도메인 모델입니다.

### 8.3.1 Profile

```kotlin
// domain/model/Profile.kt
data class Profile(
    val id: String,
    val name: String,
    val icon: String,
    val isActive: Boolean,
    val sensitivity: Float,
    val globalCooldownMs: Int,
    val triggers: List<Trigger>,
    val createdAt: Long,
    val updatedAt: Long
)
```

### 8.3.2 Trigger

```kotlin
// domain/model/Trigger.kt
data class Trigger(
    val id: String,
    val profileId: String,
    val name: String,
    val blendShape: String,          // 블렌드쉐이프 ID
    val threshold: Float,
    val holdDurationMs: Int,
    val cooldownMs: Int,
    val action: Action,
    val isEnabled: Boolean,
    val priority: Int
)
```

### 8.3.3 Action (sealed class)

```kotlin
// domain/model/Action.kt
sealed class Action {

    // ── 시스템 액션 ──────────────────────────────
    object GlobalHome : Action()
    object GlobalBack : Action()
    object GlobalRecents : Action()
    object GlobalNotifications : Action()
    object GlobalQuickSettings : Action()
    object ScreenLock : Action()
    object TakeScreenshot : Action()
    object PowerDialog : Action()

    // ── 탭/클릭 ──────────────────────────────────
    object TapCenter : Action()
    data class TapCustom(val x: Float, val y: Float) : Action()
    data class DoubleTap(val x: Float = 0.5f, val y: Float = 0.5f) : Action()
    data class LongPress(val x: Float = 0.5f, val y: Float = 0.5f) : Action()

    // ── 스와이프 ──────────────────────────────────
    data class SwipeUp(val duration: Long = 300L) : Action()
    data class SwipeDown(val duration: Long = 300L) : Action()
    data class SwipeLeft(val duration: Long = 300L) : Action()
    data class SwipeRight(val duration: Long = 300L) : Action()

    // ── 스크롤 ──────────────────────────────────
    object ScrollUp : Action()
    object ScrollDown : Action()

    // ── 드래그 / 핀치 ────────────────────────────
    data class Drag(
        val startX: Float, val startY: Float,
        val endX: Float, val endY: Float,
        val duration: Long = 500L
    ) : Action()
    object PinchIn : Action()
    object PinchOut : Action()

    // ── 앱 ──────────────────────────────────────
    data class OpenApp(val packageName: String) : Action()

    // ── 미디어 / 볼륨 ────────────────────────────
    object MediaPlayPause : Action()
    object MediaNext : Action()
    object MediaPrev : Action()
    object VolumeUp : Action()
    object VolumeDown : Action()

    // ── MimicEase 내부 ───────────────────────────
    object MimicPause : Action()
}
```

---

## 8.4 Action 직렬화 (TriggerEntity.actionParams JSON)

`TriggerEntity`에는 `actionType: String`과 `actionParams: String (JSON)` 두 필드로 Action을 저장합니다.

### 8.4.1 actionType 값 목록

```
"GlobalHome", "GlobalBack", "GlobalRecents", "GlobalNotifications",
"GlobalQuickSettings", "ScreenLock", "TakeScreenshot", "PowerDialog",
"TapCenter", "TapCustom", "DoubleTap", "LongPress",
"SwipeUp", "SwipeDown", "SwipeLeft", "SwipeRight",
"ScrollUp", "ScrollDown",
"Drag", "PinchIn", "PinchOut",
"OpenApp",
"MediaPlayPause", "MediaNext", "MediaPrev", "VolumeUp", "VolumeDown",
"MimicPause"
```

### 8.4.2 actionParams JSON 예시

```json
// 파라미터 없는 액션: {}
{ }

// TapCustom
{ "x": 0.5, "y": 0.5 }

// DoubleTap
{ "x": 0.5, "y": 0.5 }

// LongPress
{ "x": 0.3, "y": 0.7 }

// SwipeUp (기본 duration 생략 가능)
{ "duration": 300 }

// Drag
{ "startX": 0.2, "startY": 0.5, "endX": 0.8, "endY": 0.5, "duration": 500 }

// OpenApp
{ "packageName": "com.android.chrome" }
```

### 8.4.3 직렬화/역직렬화 유틸리티

```kotlin
// data/model/ActionParamsDto.kt
object ActionSerializer {
    private val gson = Gson()

    fun serialize(action: Action): Pair<String, String> {
        val type = action::class.simpleName ?: "Unknown"
        val params = when (action) {
            is Action.TapCustom  -> gson.toJson(mapOf("x" to action.x, "y" to action.y))
            is Action.DoubleTap  -> gson.toJson(mapOf("x" to action.x, "y" to action.y))
            is Action.LongPress  -> gson.toJson(mapOf("x" to action.x, "y" to action.y))
            is Action.SwipeUp    -> gson.toJson(mapOf("duration" to action.duration))
            is Action.SwipeDown  -> gson.toJson(mapOf("duration" to action.duration))
            is Action.SwipeLeft  -> gson.toJson(mapOf("duration" to action.duration))
            is Action.SwipeRight -> gson.toJson(mapOf("duration" to action.duration))
            is Action.Drag       -> gson.toJson(mapOf(
                "startX" to action.startX, "startY" to action.startY,
                "endX" to action.endX, "endY" to action.endY, "duration" to action.duration
            ))
            is Action.OpenApp    -> gson.toJson(mapOf("packageName" to action.packageName))
            else                 -> "{}"
        }
        return type to params
    }

    fun deserialize(type: String, paramsJson: String): Action {
        val map = gson.fromJson(paramsJson, Map::class.java) ?: emptyMap<String, Any>()
        return when (type) {
            "GlobalHome"       -> Action.GlobalHome
            "GlobalBack"       -> Action.GlobalBack
            "GlobalRecents"    -> Action.GlobalRecents
            "TapCenter"        -> Action.TapCenter
            "TapCustom"        -> Action.TapCustom(
                x = (map["x"] as? Double)?.toFloat() ?: 0.5f,
                y = (map["y"] as? Double)?.toFloat() ?: 0.5f
            )
            "SwipeUp"          -> Action.SwipeUp((map["duration"] as? Double)?.toLong() ?: 300L)
            "SwipeDown"        -> Action.SwipeDown((map["duration"] as? Double)?.toLong() ?: 300L)
            "SwipeLeft"        -> Action.SwipeLeft((map["duration"] as? Double)?.toLong() ?: 300L)
            "SwipeRight"       -> Action.SwipeRight((map["duration"] as? Double)?.toLong() ?: 300L)
            "ScrollUp"         -> Action.ScrollUp
            "ScrollDown"       -> Action.ScrollDown
            "Drag"             -> Action.Drag(
                startX = (map["startX"] as? Double)?.toFloat() ?: 0f,
                startY = (map["startY"] as? Double)?.toFloat() ?: 0f,
                endX   = (map["endX"] as? Double)?.toFloat() ?: 0f,
                endY   = (map["endY"] as? Double)?.toFloat() ?: 0f,
                duration = (map["duration"] as? Double)?.toLong() ?: 500L
            )
            "OpenApp"          -> Action.OpenApp((map["packageName"] as? String) ?: "")
            "MediaPlayPause"   -> Action.MediaPlayPause
            "MediaNext"        -> Action.MediaNext
            "MediaPrev"        -> Action.MediaPrev
            "VolumeUp"         -> Action.VolumeUp
            "VolumeDown"       -> Action.VolumeDown
            "MimicPause"       -> Action.MimicPause
            else               -> Action.GlobalHome  // 알 수 없는 타입 폴백
        }
    }
}
```

---

## 8.5 AppSettings (DataStore)

```kotlin
// data/local/AppSettingsDataStore.kt
data class AppSettings(
    val cameraFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val emaAlpha: Float = 0.5f,              // EMA 필터 계수 (0.1~0.9)
    val consecutiveFrames: Int = 3,          // 표정 확정 필요 연속 프레임 수
    val showForegroundNotification: Boolean = true,
    val notificationTapAction: String = "OPEN_APP",  // "OPEN_APP" | "PAUSE"
    val isDeveloperMode: Boolean = false,
    val isServiceEnabled: Boolean = false,
    val activeProfileId: String? = null,
    val onboardingCompleted: Boolean = false
)

// DataStore Keys
val CAMERA_FACING        = intPreferencesKey("camera_facing")
val EMA_ALPHA            = floatPreferencesKey("ema_alpha")
val CONSECUTIVE_FRAMES   = intPreferencesKey("consecutive_frames")
val SHOW_NOTIFICATION    = booleanPreferencesKey("show_notification")
val DEVELOPER_MODE       = booleanPreferencesKey("developer_mode")
val SERVICE_ENABLED      = booleanPreferencesKey("service_enabled")
val ACTIVE_PROFILE_ID    = stringPreferencesKey("active_profile_id")
val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
```

---

## 8.6 Mapper (Entity ↔ Domain)

```kotlin
// data/repository/ProfileRepositoryImpl.kt (내부 매퍼)
fun ProfileWithTriggers.toDomain(): Profile = Profile(
    id = profile.id,
    name = profile.name,
    icon = profile.icon,
    isActive = profile.isActive,
    sensitivity = profile.sensitivity,
    globalCooldownMs = profile.globalCooldownMs,
    triggers = triggers.map { it.toDomain() },
    createdAt = profile.createdAt,
    updatedAt = profile.updatedAt
)

fun TriggerEntity.toDomain(): Trigger = Trigger(
    id = id,
    profileId = profileId,
    name = name,
    blendShape = blendShape,
    threshold = threshold,
    holdDurationMs = holdDurationMs,
    cooldownMs = cooldownMs,
    action = ActionSerializer.deserialize(actionType, actionParams),
    isEnabled = isEnabled,
    priority = priority
)

fun Trigger.toEntity(): TriggerEntity {
    val (type, params) = ActionSerializer.serialize(action)
    return TriggerEntity(
        id = id, profileId = profileId, name = name,
        blendShape = blendShape, threshold = threshold,
        holdDurationMs = holdDurationMs, cooldownMs = cooldownMs,
        actionType = type, actionParams = params,
        isEnabled = isEnabled, priority = priority
    )
}
```
