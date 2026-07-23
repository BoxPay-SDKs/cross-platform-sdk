package com.crossplatform.sdk.data.model


import com.crossplatform.sdk.data.model.requestBody.ShopperRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
data class MethodsPostRequest(
    @SerialName("browserData") val browserData: BrowserData,
    @SerialName("instrumentDetails") val instrumentDetails: MethodInstrumentDetails,
    @SerialName("shopper") val shopper: ShopperRequest,
    @SerialName("deviceDetails") val deviceDetails: DeviceDetails
)

@Serializable(with = MethodInstrumentDetailsSerializer::class)
data class MethodInstrumentDetails(
    val type: String,
    val paymentType : String,
    val details: Details
)

@Serializable
data class Details(
    @SerialName("token") val token: String
)

object MethodInstrumentDetailsSerializer : KSerializer<MethodInstrumentDetails> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("MethodInstrumentDetails")

    override fun serialize(encoder: Encoder, value: MethodInstrumentDetails) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: error("This serializer only works with Json")

        val jsonObject = buildJsonObject {
            put("type", value.type)
            put(value.paymentType, jsonEncoder.json.encodeToJsonElement(Details.serializer(), value.details))
        }

        jsonEncoder.encodeJsonElement(jsonObject)
    }

    override fun deserialize(decoder: Decoder): MethodInstrumentDetails {
        val jsonDecoder = decoder as? JsonDecoder
            ?: error("This serializer only works with Json")

        val jsonObject = jsonDecoder.decodeJsonElement().jsonObject
        val type = jsonObject["type"]?.jsonPrimitive?.content
            ?: error("Missing 'type' field")

        val detailsElement = jsonObject[type]
            ?: error("Missing '$type' field matching 'type' value")

        val details = jsonDecoder.json.decodeFromJsonElement(Details.serializer(), detailsElement)

        return MethodInstrumentDetails(type, "" , details)
    }
}