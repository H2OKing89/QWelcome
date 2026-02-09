package com.kingpaging.qwelcome.data

import android.util.Log

private const val TAG = "SettingsStoreMappers"
private const val MAX_PROFILE_FIELD_LENGTH = 500

fun TechProfile.toProto(): TechProfileProto {
    val truncatedName = name.take(MAX_PROFILE_FIELD_LENGTH)
    val truncatedTitle = title.take(MAX_PROFILE_FIELD_LENGTH)
    val truncatedDept = dept.take(MAX_PROFILE_FIELD_LENGTH)

    if (name.length > MAX_PROFILE_FIELD_LENGTH) {
        Log.w(TAG, "TechProfile name truncated from ${name.length} to $MAX_PROFILE_FIELD_LENGTH chars")
    }
    if (title.length > MAX_PROFILE_FIELD_LENGTH) {
        Log.w(TAG, "TechProfile title truncated from ${title.length} to $MAX_PROFILE_FIELD_LENGTH chars")
    }
    if (dept.length > MAX_PROFILE_FIELD_LENGTH) {
        Log.w(TAG, "TechProfile dept truncated from ${dept.length} to $MAX_PROFILE_FIELD_LENGTH chars")
    }

    return TechProfileProto.newBuilder()
        .setName(truncatedName)
        .setTitle(truncatedTitle)
        .setDept(truncatedDept)
        .build()
}

fun TechProfile.Companion.fromProto(proto: TechProfileProto): TechProfile {
    val truncatedName = proto.name.take(MAX_PROFILE_FIELD_LENGTH)
    val truncatedTitle = proto.title.take(MAX_PROFILE_FIELD_LENGTH)
    val truncatedDept = proto.dept.take(MAX_PROFILE_FIELD_LENGTH)

    return TechProfile(
        name = truncatedName,
        title = truncatedTitle,
        dept = truncatedDept
    )
}

val TechProfile.Companion.empty: TechProfile
    get() = TechProfile()

fun Template.toProto(): TemplateProto = TemplateProto.newBuilder()
    .setId(id)
    .setName(name)
    .setContent(content)
    .setCreatedAt(createdAt)
    .setModifiedAt(modifiedAt)
    .setSlug(slug ?: "")
    .build()

fun Template.Companion.fromProto(proto: TemplateProto): Template = Template(
    id = proto.id,
    name = proto.name,
    content = proto.content,
    // Use epoch as deterministic default for missing timestamps (not Instant.now()).
    createdAt = proto.createdAt.ifEmpty { "1970-01-01T00:00:00Z" },
    modifiedAt = proto.modifiedAt.ifEmpty { "1970-01-01T00:00:00Z" },
    slug = proto.slug.ifEmpty { null }
)
