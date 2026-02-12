package hoon.example.androidsandbox.data.kvs

import hoon.example.androidsandbox.BuildConfig

data class KvsClientConfig(
    val accessKey: String = BuildConfig.AWS_ACCESS_KEY,
    val secretKey: String = BuildConfig.AWS_SECRET_KEY,
    val region: String = BuildConfig.AWS_REGION,
    val channelName: String = BuildConfig.KVS_CHANNEL_NAME
)
