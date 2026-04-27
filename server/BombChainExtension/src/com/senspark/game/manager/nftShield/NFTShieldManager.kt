package com.senspark.game.manager.nftShield

import com.senspark.common.cache.ICacheService
import com.senspark.game.db.nftShield.INFTShieldDataAccess
import com.senspark.game.manager.IEnvManager
import org.web3j.crypto.Credentials
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import java.security.MessageDigest
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.abi.datatypes.DynamicArray
import org.web3j.crypto.Hash
import java.nio.ByteBuffer
import java.math.BigInteger

class NFTShieldManager(
    private val dataAccess: INFTShieldDataAccess,
    private val envManager: IEnvManager,
    private val cacheService: ICacheService
) : INFTShieldManager {

    private val maxAttempts = 3
    private val lockDurationSecs = 86400L // 24 hours

    override fun initialize() {}

    override fun getShieldStatus(uid: Int): Boolean {
        return dataAccess.getPinHash(uid) != null
    }

    override fun setupPin(uid: Int, pin: String) {
        require(pin.length == 4 && pin.all { it.isDigit() }) { "PIN must be 4 digits" }
        val pinHash = hashPin(pin, uid.toString())
        dataAccess.setupPin(uid, pinHash)
    }

    override fun verifyPin(uid: Int, pin: String): Boolean {
        require(!isLocked(uid)) { "Wallet shield is currently locked due to too many failed attempts" }

        val storedHash = dataAccess.getPinHash(uid) ?: return false
        val inputHash = hashPin(pin, uid.toString())

        if (storedHash == inputHash) {
            dataAccess.resetFailedAttempts(uid)
            return true
        } else {
            dataAccess.recordFailedAttempt(uid, maxAttempts, lockDurationSecs)
            return false
        }
    }

    override fun isLocked(uid: Int): Boolean {
        return dataAccess.isLocked(uid)
    }

    override fun getLockUntil(uid: Int): Long? {
        return dataAccess.getLockUntil(uid)
    }

    override fun getFailedAttempts(uid: Int): Int {
        return dataAccess.getFailedAttempts(uid)
    }

    override fun generateSignature(walletAddress: String, nonce: Long, tokenIds: List<Long>): String {
        // web3j equivalent to:
        // keccak256(abi.encodePacked(walletAddress, nonce, tokenIds))
        val addressBytes = Numeric.hexStringToByteArray(walletAddress)
        val nonceBytes = ByteBuffer.allocate(32).put(Numeric.toBytesPadded(BigInteger.valueOf(nonce), 32)).array()
        
        // Convert tokenIds into uint256 array byte packed
        val tokenBytes = ByteBuffer.allocate(tokenIds.size * 32)
        for (id in tokenIds) {
            tokenBytes.put(Numeric.toBytesPadded(BigInteger.valueOf(id), 32))
        }

        val packedBytes = ByteBuffer.allocate(addressBytes.size + nonceBytes.size + tokenBytes.capacity())
            .put(addressBytes)
            .put(nonceBytes)
            .put(tokenBytes.array())
            .array()

        val hashBytes = Hash.sha3(packedBytes)
        
        // EIP-191 signature
        val credentials = Credentials.create(envManager.shieldPrivateKey)
        val signatureData = Sign.signPrefixedMessage(hashBytes, credentials.ecKeyPair)
        
        val r = signatureData.r
        val s = signatureData.s
        val v = signatureData.v
        
        val rStr = Numeric.toHexString(r).drop(2).padStart(64, '0')
        val sStr = Numeric.toHexString(s).drop(2).padStart(64, '0')
        val vStr = Numeric.toHexString(v).drop(2).padStart(2, '0')
        
        return "0x" + rStr + sStr + vStr
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = pin + salt + envManager.messageSalt
        val hashBytes = md.digest(combined.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
