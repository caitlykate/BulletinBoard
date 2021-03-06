package com.caitlykate.bulletinboard.model

import java.io.Serializable

data class Ad(
    val country: String? = null,
    val city: String? = null,
    val tel: String? = null,
    val email: String? = null,
    val withSend: String? = null,
    val title: String? = null,
    val category: String? = null,
    val price: String? = null,
    val description: String? = null,

    val key: String? = null,
    val uid: String? = null,

    val mainImage: String? = null,
    val image2: String? = null,
    val image3: String? = null,

    var time: String = "0",

    var isFav: Boolean = false,
    var favCounter: String = "0",

    var viewsCounter: String = "0",
    var emailsCounter: String = "0",
    var callsCounter: String = "0",


): Serializable
