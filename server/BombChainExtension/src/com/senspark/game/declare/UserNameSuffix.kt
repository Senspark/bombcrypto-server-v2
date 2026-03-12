package com.senspark.game.declare

class UserNameSuffix {
    companion object {
        fun removeSuffixName(userName: String): String {
            return if (isWallet(userName)) removeSuffixNameWallet(userName)
            else removeSuffixNameAccount(userName)
        }


        fun tryAddNameSuffix(userName: String, dataType: EnumConstants.DataType): String {
            return if (isWallet(userName, dataType)) tryAddNameSuffixWallet(userName, dataType)
            else tryAddNameSuffixAccount(userName, dataType)
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

        // Dùng để remove suffix cho userName của account fi bsc, polygon
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