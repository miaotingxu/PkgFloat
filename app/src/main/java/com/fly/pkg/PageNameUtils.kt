package com.fly.pkg

object PageNameUtils {

    fun resolveFullClassName(packageName: String, className: String): String =
        if (className.startsWith(packageName)) className else packageName + className

    fun shouldIgnore(packageName: String, selfPackageName: String): Boolean =
        packageName == selfPackageName
}
