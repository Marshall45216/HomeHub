package com.familycal.tv.auth

import android.content.Context
import com.microsoft.identity.client.*
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Handles Microsoft sign-in for each family member using the DEVICE CODE FLOW.
 *
 * Why device code flow: the Fire TV remote has no keyboard. Instead of typing
 * an email/password on screen, the app shows a short code (e.g. "ABCD-1234")
 * and a URL (microsoft.com/devicelogin). The family member opens that URL on
 * their phone, enters the code once, and the TV app silently receives a token.
 * MSAL then caches it so re-login isn't needed on every launch.
 *
 * Each family member is signed in once, added to the family, and their token
 * is refreshed automatically in the background.
 */
class AuthManager(context: Context) {

    // authConfig.json (see /app/src/main/res/raw or assets) holds the client ID,
    // redirect URI, and authority — generated from your Azure AD app registration.
    // See README.md "Azure setup" section for how to create this.
    private val app: IPublicClientApplication = PublicClientApplication.create(
        context,
        com.familycal.tv.R.raw.auth_config
    )

    /**
     * Starts device-code sign-in for one family member.
     * [onCodeReady] is called immediately with the code + URL to display on screen.
     * The suspend function itself completes once the person finishes signing in
     * on their phone/laptop (it polls Microsoft in the background).
     */
    suspend fun signInFamilyMember(
        scopes: List<String> = listOf("Calendars.ReadWrite", "User.Read"),
        onCodeReady: (userCode: String, verificationUri: String) -> Unit
    ): IAuthenticationResult = suspendCancellableCoroutine { cont ->
        (app as IPublicClientApplication).acquireTokenWithDeviceCode(
            scopes,
            object : IPublicClientApplication.DeviceCodeFlowCallback {
                override fun onUserCodeReceived(
                    vUri: String,
                    userCode: String,
                    message: String,
                    expiresIn: java.util.Date
                ) {
                    onCodeReady(userCode, vUri)
                }

                override fun onTokenReceived(authResult: IAuthenticationResult) {
                    if (cont.isActive) cont.resume(authResult)
                }

                override fun onError(exception: MsalException) {
                    if (cont.isActive) cont.resumeWithException(exception)
                }
            }
        )
    }

    /** Silently refreshes a token for an already-signed-in account. Call before each Graph API batch. */
      suspend fun getFreshToken(account: IAccount, scopes: List<String>): String =
        suspendCancellableCoroutine { cont ->
            val parameters = AcquireTokenSilentParameters.Builder()
                .withScopes(scopes)
                .forAccount(account)
                .fromAuthority(account.authority)
                .withCallback(object : SilentAuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        cont.resume(authenticationResult.accessToken)
                    }
                    override fun onError(exception: MsalException) {
                        cont.resumeWithException(exception)
                    }
                })
                .build()
            app.acquireTokenSilentAsync(parameters)
        }

    fun getSignedInAccounts(): List<IAccount> =
        (app as? IMultipleAccountPublicClientApplication)?.accounts ?: emptyList()
}
