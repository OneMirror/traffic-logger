package bot.inker.onemirror.traffic

object SecurityManager {
    private val constSecret = OneMirrorProperties["secret"]
    fun auth(name:String, secret:String):Boolean{
        return secret == constSecret
    }
}