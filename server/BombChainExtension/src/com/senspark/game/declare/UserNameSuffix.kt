package com.senspark.game.declare

class UserNameSuffix {
    companion object {
        // Separator dành riêng cho suffix server-side (vd adventure "<ví>#tr"). '#' là ký tự reserved
        // trong codebase (NEW_LOGIN_NAME, sessionKey) — username thật (ví hex / account) không chứa '#'
        // nên cắt từ '#' là an toàn tuyệt đối, không đụng logic endsWith mong manh.
        const val SUFFIX_SEPARATOR = "#"

        fun removeSuffixName(userName: String): String {
            if (userName.contains(SUFFIX_SEPARATOR)) return userName.substringBefore(SUFFIX_SEPARATOR)
            return if (isWallet(userName)) removeSuffixNameWallet(userName)
            else removeSuffixNameAccount(userName)
        }


        fun tryAddNameSuffix(userName: String, dataType: EnumConstants.DataType): String {
            return if (isWallet(userName, dataType)) tryAddNameSuffixWallet(userName, dataType)
            else tryAddNameSuffixAccount(userName, dataType)
        }

        // Suffix chuẩn mới có separator '#': "<ví>#<network>" (vd "0x..fa9#bsc", "..#polygon", "..#tr").
        // Strip an toàn tuyệt đối bằng removeSuffixName (cắt từ '#'). Dùng cho identity phiên FI/adventure.
        // KHÔNG dùng cho deposit-matching airdrop (VIC/RON/BAS) — chỗ đó vẫn dùng tryAddNameSuffix (không separator).
        fun addSuffixWithSeparator(userName: String, dataType: EnumConstants.DataType): String {
            return removeSuffixName(userName) + SUFFIX_SEPARATOR + dataType.name.lowercase()
        }


        private fun isWallet(userName: String, dataType: EnumConstants.DataType = EnumConstants.DataType.BSC): Boolean{
            if(userName.length == 42 && userName.startsWith("0x") && dataType.isEthereumUser()){
                return true
            }
            return false
        }

        private fun removeSuffixNameWallet(userName: String): String {
            // EVM wallet always have 42 characters
            return if (userName.length > 42) userName.substring(0, 42) else userName
        }


        private fun tryAddNameSuffixWallet(userName: String, dataType: EnumConstants.DataType): String {
            if(!dataType.isEthereumUser()){
                // tính năng này chỉ áp dụng cho EVM wallet
                return userName
            }
            // EVM wallet always have 42 characters
            return if (userName.length == 42) {
                val suffix = dataType.name.lowercase()
                userName + suffix
            } else {
                userName
            }
        }

        // Dùng để remove suffix cho userName của account fi bsc, polygon (client nối trực tiếp, không separator)
        private fun removeSuffixNameAccount(userName: String): String {
            if(userName.endsWith("bsc")){
                return userName.dropLast(3)
            }
            if(userName.endsWith("polygon")){
                return userName.dropLast(7)
            }
            return userName
        }

        private fun tryAddNameSuffixAccount(userName: String, dataType: EnumConstants.DataType): String {
            if(dataType != EnumConstants.DataType.BSC && dataType != EnumConstants.DataType.POLYGON){
                // tính năng này chỉ áp dụng cho account fi của bsc và polygon
                return userName
            }
            if(userName.endsWith(dataType.name.lowercase())){
                return userName
            }
            return userName + dataType.name.lowercase()
        }
    }
}