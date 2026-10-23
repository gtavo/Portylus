package com.gtavoapps.portylus
import com.gtavoapps.portylus.storage.VerifiedDocumentWrite
import com.gtavoapps.portylus.storage.DocumentWriteFailure
import java.io.*
import org.junit.Assert.*
import org.junit.Test
class VerifiedDocumentWriteTest {
    private fun withFiles(block: (File,File,File)->Unit) {
        val folder = java.nio.file.Files.createTempDirectory("portylus-save-").toFile()
        try {
            val target=File(folder,"target.pdf").apply { writeText("original bytes are longer") }
            val prepared=File(folder,"prepared.pdf").apply { writeText("new PDF") }
            block(target,prepared,File(folder,"backup.pdf"))
        } finally { folder.deleteRecursively() }
    }
    @Test fun successfulWriteVerifiesAllBytesAndRemovesBackup() = withFiles { target,new,backup ->
        val result=VerifiedDocumentWrite.write(new,backup,VerifiedDocumentWrite.hash(target),
            {target.inputStream()},{target.outputStream()})
        assertArrayEquals(new.readBytes(),target.readBytes()); assertEquals(VerifiedDocumentWrite.hash(new),result); assertFalse(backup.exists())
    }
    @Test fun conflictNeverOpensTheDestinationForWriting() = withFiles { target,new,backup ->
        val expected=VerifiedDocumentWrite.hash(target); target.writeText("changed elsewhere")
        var opened=false
        assertThrows(DocumentWriteFailure::class.java) {
            VerifiedDocumentWrite.write(new,backup,expected,{target.inputStream()},{opened=true;target.outputStream()})
        }
        assertFalse(opened); assertEquals("changed elsewhere",target.readText()); assertFalse(backup.exists())
    }
    @Test fun partialWriteRestoresExactOriginalAndDoesNotReportSuccess() = withFiles { target,new,backup ->
        val original=target.readBytes(); var attempts=0
        val error=assertThrows(DocumentWriteFailure::class.java) {
            VerifiedDocumentWrite.write(new,backup,null,{target.inputStream()},{
                attempts++
                if(attempts==1) object: FilterOutputStream(target.outputStream()) {
                    override fun write(b:ByteArray,off:Int,len:Int) { out.write(b,off,2); throw IOException("interrupted") }
                } else target.outputStream()
            })
        }
        assertArrayEquals(original,target.readBytes()); assertNull(error.recovery); assertFalse(backup.exists())
    }
    @Test fun providerThatDoesNotTruncateFailsReadbackAndRestores() = withFiles { target,new,backup ->
        val original=target.readBytes(); var attempt=0
        assertThrows(DocumentWriteFailure::class.java) {
            VerifiedDocumentWrite.write(new,backup,null,{target.inputStream()},{
                attempt++
                if(attempt==1) object: OutputStream() {
                    val file=RandomAccessFile(target,"rw")
                    override fun write(b:Int)=file.write(b)
                    override fun close()=file.close()
                } else target.outputStream()
            })
        }
        assertArrayEquals(original,target.readBytes())
    }
    @Test fun writeDeniedLeavesOriginalUntouched() = withFiles { target,new,backup ->
        val original=target.readBytes()
        val error=assertThrows(DocumentWriteFailure::class.java) {
            VerifiedDocumentWrite.write(new,backup,null,{target.inputStream()},{throw SecurityException("read only")})
        }
        assertArrayEquals(original,target.readBytes()); assertNull(error.recovery); assertFalse(backup.exists())
    }
    @Test fun failedWriteAndFailedRestoreKeepPrivateRecoveryBytes() = withFiles { target,new,backup ->
        val original=target.readBytes()
        val error=assertThrows(DocumentWriteFailure::class.java) {
            VerifiedDocumentWrite.write(new,backup,null,{target.inputStream()},{
                object: FilterOutputStream(target.outputStream()) {
                    override fun write(b:ByteArray,off:Int,len:Int) { out.write(b,off,1);throw IOException("failed") }
                }
            })
        }
        assertEquals(backup,error.recovery); assertArrayEquals(original,backup.readBytes())
    }
}
