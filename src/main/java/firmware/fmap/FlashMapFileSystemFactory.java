/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package firmware.fmap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import ghidra.app.util.bin.ByteProvider;
import ghidra.formats.gfilesystem.FSRLRoot;
import ghidra.formats.gfilesystem.FileSystemService;
import ghidra.formats.gfilesystem.factory.GFileSystemFactoryByteProvider;
import ghidra.formats.gfilesystem.factory.GFileSystemProbeByteProvider;
import ghidra.util.Msg;
import ghidra.util.task.TaskMonitor;

public class FlashMapFileSystemFactory implements GFileSystemFactoryByteProvider<FlashMapFileSystem>,
		GFileSystemProbeByteProvider {
	@Override
	public boolean probe(ByteProvider provider, FileSystemService fsService, TaskMonitor monitor) throws IOException {
		return findFMapSignatureOffset(provider) != -1;
	}

	@Override
	public FlashMapFileSystem create(FSRLRoot fsrlRoot, ByteProvider provider, FileSystemService fsService,
			TaskMonitor monitor) throws IOException {
		FlashMapFileSystem fs = new FlashMapFileSystem(fsrlRoot);
		try {
			long offset = findFMapSignatureOffset(provider);
			if (offset < 0) {
				throw new IOException("FMAP signature not found");
			}

			fs.mount(provider, offset, monitor);
			return fs;
		} catch (IOException ioe) {
			fs.close();
			throw ioe;
		}
	}

	private long findFMapSignatureOffset(ByteProvider provider) throws IOException {
		long remainingLength = provider.length();
		long offset = 0;
		while (remainingLength >= FlashMapConstants.FMAP_SIGNATURE.length()) {
			String signature = new String(provider.readBytes(offset, FlashMapConstants.FMAP_SIGNATURE.length()),
					StandardCharsets.US_ASCII);
			if (signature.equals(FlashMapConstants.FMAP_SIGNATURE)) {
				Msg.debug(this, String.format("Found FMAP signature at 0x%X", offset));
				return offset;
			}

			offset += FlashMapConstants.FMAP_SIGNATURE.length();
			remainingLength -= FlashMapConstants.FMAP_SIGNATURE.length();
		}

		return -1;
	}
}
