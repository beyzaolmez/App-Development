package com.nhlstenden.momentum.util



import com.google.firebase.auth.FirebaseAuthException



/**

 * Single source of truth for the user-facing error copy shown throughout the app.

 *

 * Goals (see "Friendly Error Messages" task):

 *  • avoid technical jargon and raw exception text,

 *  • give people an understandable next step,

 *  • keep wording consistent everywhere.

 *

 * The string-mapping core ([auth], [firestoreSync], [profileSave], [questFeedbackSave])

 * is intentionally pure Kotlin with no Android/Firebase types, so it can be unit-tested

 * directly on the JVM. The [Throwable] extensions at the bottom are thin adapters that

 * pull the diagnostic details out of a real exception and delegate to the pure core.

 */

object FriendlyErrorMessages {



    /** Fallback used when nothing more specific matches an auth failure. */

    private fun authFallback(action: String) = "$action didn't work. Please try again."



    /**

     * Friendly copy for an authentication failure (sign in / register).

     *

     * @param errorCode the Firebase error code, if any (e.g. "ERROR_WRONG_PASSWORD").

     * @param rawMessage the exception's message, used only to sniff for keywords.

     * @param action human-readable action for the fallback, e.g. "Sign in" or "Registration".

     */

    fun auth(errorCode: String?, rawMessage: String?, action: String): String {

        val diagnostic = "${errorCode.orEmpty()} ${rawMessage.orEmpty()}".uppercase()

        return when {

            "CONFIGURATION_NOT_FOUND" in diagnostic ->

                "Accounts aren't enabled for this test build yet. You can continue without an account for now."

            "EMAIL_ALREADY_IN_USE" in diagnostic ->

                "This email already has an account. Try signing in instead."

            "INVALID_LOGIN_CREDENTIALS" in diagnostic ||

                "INVALID_CREDENTIAL" in diagnostic ||

                "WRONG_PASSWORD" in diagnostic ||

                "USER_NOT_FOUND" in diagnostic ->

                "That email or password doesn't match. Please check them and try again."

            "INVALID_EMAIL" in diagnostic ->

                "That doesn't look like a valid email address. Please check it and try again."

            "USER_DISABLED" in diagnostic ->

                "This account has been disabled. Please contact support if you think this is a mistake."

            "WEAK_PASSWORD" in diagnostic ->

                "Please choose a stronger password with at least 6 characters."

            "TOO_MANY_REQUESTS" in diagnostic ->

                "Too many attempts in a row. Please wait a moment and try again."

            "NETWORK" in diagnostic ->

                "Can't reach the network. Check your connection and try again."

            else -> authFallback(action)

        }

    }



    /**

     * Friendly copy for a Firestore quest load/sync failure. The app always has a demo

     * fallback, so the wording reassures the user their experience continues meanwhile.

     */

    fun firestoreSync(rawMessage: String?): String {

        val diagnostic = rawMessage.orEmpty().uppercase()

        return when {

            "PERMISSION_DENIED" in diagnostic || "CLOUD FIRESTORE API" in diagnostic ->

                "We couldn't reach your saved quests, so we're showing demo quests for now."

            "UNAVAILABLE" in diagnostic || "NETWORK" in diagnostic ->

                "You appear to be offline. We're showing demo quests until you reconnect."

            "DEADLINE_EXCEEDED" in diagnostic || "TIMEOUT" in diagnostic ->

                "Loading your quests is taking longer than usual. We're showing demo quests for now."

            else -> "We couldn't sync your quests right now. We're showing demo quests instead."

        }

    }



    /** Friendly copy for a profile (display name) save failure. */

    fun profileSave(): String =

        "We couldn't save your changes right now. Please try again."



    /** Friendly copy for a failed quest feedback (like/dislike) save. */

    fun questFeedbackSave(): String =

        "We couldn't save your quest feedback. Please try again."



    /** Friendly copy for account deletion failure. */

    fun accountDeletion(rawMessage: String?): String {

        val diagnostic = rawMessage.orEmpty().uppercase()

        return when {

            "NETWORK" in diagnostic || "UNAVAILABLE" in diagnostic ->

                "Can't reach the network. Check your connection and try again."

            "REQUIRES_RECENT_LOGIN" in diagnostic ->

                "Please sign out and sign back in, then try deleting your account again."

            else -> "We couldn't delete your account right now. Please try again later."

        }

    }



    /**

     * Friendly copy for a password-reset email failure.

     *

     * @param errorCode the Firebase error code, if any.

     * @param rawMessage the exception's message, used only to sniff for keywords.

     */

    fun passwordReset(errorCode: String?, rawMessage: String?): String {

        val diagnostic = "${errorCode.orEmpty()} ${rawMessage.orEmpty()}".uppercase()

        return when {

            "INVALID_EMAIL" in diagnostic ->

                "That doesn't look like a valid email. Please check it and try again."

            "USER_NOT_FOUND" in diagnostic ->

                "No account found for that email. Check the address or create an account."

            "TOO_MANY_REQUESTS" in diagnostic ->

                "Too many attempts in a row. Please wait a moment and try again."

            "NETWORK" in diagnostic ->

                "Can't reach the network. Check your connection and try again."

            else -> "We couldn't send the reset link. Please try again."

        }

    }

}



/** Maps any auth exception to friendly copy, extracting the Firebase error code when present. */

fun Throwable.toFriendlyAuthMessage(action: String): String =

    FriendlyErrorMessages.auth(

        errorCode = (this as? FirebaseAuthException)?.errorCode,

        rawMessage = localizedMessage,

        action = action

    )



/** Maps any quest load/sync exception to friendly copy. */

fun Throwable.toFriendlyQuestDataMessage(): String =

    FriendlyErrorMessages.firestoreSync(localizedMessage)



/** Maps any password-reset exception to friendly copy. */

fun Throwable.toFriendlyPasswordResetMessage(): String =

    FriendlyErrorMessages.passwordReset(

        errorCode = (this as? FirebaseAuthException)?.errorCode,

        rawMessage = localizedMessage

    )



/** Maps any exception to friendly copy for account deletion. */
fun Throwable.toFriendlyAccountDeletionMessage(): String =
    FriendlyErrorMessages.accountDeletion(localizedMessage)
