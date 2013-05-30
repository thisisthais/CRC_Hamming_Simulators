import java.math.BigInteger;
import java.util.Arrays;

// =============================================================================
/**
 * A data link layer where blocks of data entering the network get a short check
 * value attached, based on the remainder of a polynomial division of their
 * contents; on retrieval the calculation is repeated, and corrective action can
 * be taken against presumed data corruption if the check values do not match.
 **/

public class CRCDataLinkLayer extends DataLinkLayer {
	// =============================================================================

	// =========================================================================
	/**
	 * The constructor. Make a new CRC data link layer.
	 * 
	 * @param physicalLayer
	 *            The physical layer through which this data link layer should
	 *            communicate.
	 **/
	public CRCDataLinkLayer(PhysicalLayer physicalLayer) {

		// Initialize the layer.
		initialize(physicalLayer);

	} // ParityDataLinkLayer
		// =========================================================================

	// =========================================================================
	/**
	 * Accept a buffer of data to send. Send it as divided into multiple frames
	 * of a fixed, maximum size. Add a parity bit for error checking to each
	 * frame. Call the physical layer to actually send each frame.
	 * 
	 * @param data
	 *            An array of bytes to be framed and transmitted.
	 **/
	public void send(byte[] data) {

		// Calculate the number of frames needed to transmit this data.
		int numberFrames = (int) Math
				.ceil((double) data.length / _maxFrameSize);

		// Construct each frame and send it.
		for (int frameNumber = 0; frameNumber < numberFrames; frameNumber++) {

			int beginIndex = _maxFrameSize * frameNumber;
			int endIndex = _maxFrameSize * (frameNumber + 1);
			if (endIndex > data.length) {
				endIndex = data.length;
			}
			byte[] frame = constructFrame(data, beginIndex, endIndex);
			physicalLayer.send(frame);

		}

	} // send (byte[] data)
		// =========================================================================

	// =========================================================================
	/**
	 * Create a single frame to be transmitted.
	 * 
	 * @param data
	 *            The original buffer of data from which to extract a frame's
	 *            worth.
	 * @param begin
	 *            The starting index from the original data buffer.
	 * @param end
	 *            The ending index from the original frame buffer.
	 * @return A byte array that contains an entirely constructed frame.
	 **/
	private byte[] constructFrame(byte[] data, int begin, int end) {

	

		// Allocate an array of bytes large enough to hold the largest possible
		// frame (tags and parity byte included).
		byte[] framedData = new byte[(_maxFrameSize * 2) + _generator.length()
				- 1];

		// Begin with the start tag.
		int frameIndex = 0;
		framedData[frameIndex++] = _startTag;

		// Add each byte of original data.
		for (int dataIndex = begin; dataIndex < end; dataIndex++) {

			// If the current data byte is itself a metadata tag, then preceed
			// it with an escape tag.
			byte currentByte = data[dataIndex];
			if ((currentByte == _startTag) || (currentByte == _stopTag)
					|| (currentByte == _escapeTag)) {

				framedData[frameIndex++] = _escapeTag;

			}

			// Add the data byte itself.
			framedData[frameIndex++] = currentByte;

		}
		

		// Calculate the remainder and append it.
		BitVector r = new BitVector(data,begin,end);
		BitVector rem = calculateRemainder(r,true);	
				
		byte[] byteRemainder = rem.toByteArray();	
		for (int i = 0; i < byteRemainder.length; i++) {
			framedData[frameIndex++] = byteRemainder[i];
		}

		// End with a stop tag.
		framedData[frameIndex++] = _stopTag;

		// Copy the complete frame into a buffer of the exact desired
		// size.
		byte[] finalFrame = new byte[frameIndex];
		for (int i = 0; i < frameIndex; i++) {
			finalFrame[i] = framedData[i];
		}
		//System.out.println("Calculated message with remainder: " + Arrays.toString(finalFrame));
		return finalFrame;

	} // constructFrame (byte[] data, int begin, int end)
		// =========================================================================

	// ==========================================================================
	public BitVector calculateRemainder(BitVector dividend, boolean z) {
	

		BitVector remainder = new BitVector();
		


		int size = dividend.length();
		
				// Append 0's
		
				// System.out.println("This is the dividend length:" + size);
				for (int i = size; i < size + _generator.length() - 1; i++) {
					dividend.setBit(i, false);
				}
				// System.out.println("Appended 0's");

			
		
		size = dividend.length();
		for (int i = 0; i < size; i++) {
			remainder.setBit(i, dividend.getBit(i));
		}
		//print ("starting remainder", remainder);
		
		
		//Print the dividend
				//print("dividend", dividend);
		
		//Print the generator
				//print("generator", _generator);
		
		//System.out.println();
		int currentIndex = 0;
		
		while (size-currentIndex>= _generator.length()) {
			
			//System.out.println("This is the dividend length: " + remainder.length());
			//print("dividend", dividend);
			
			//System.out.println("Stuck in remainder while loop?");
			for (int i = 0; i < _generator.length(); i++) {
				boolean bit = remainder.getBit(i+currentIndex)^ _generator.getBit(i);
				/*System.out.print("This is the XORed bit: ");
				if (bit) {
					System.out.print("1");
				} else {
					System.out.print("0");
				}*/
				remainder.setBit(i+currentIndex,bit);
				//System.out.println();
			}
				//print("current remainder", remainder);
			
				while ((currentIndex != size - 1) && ((remainder.getBit(currentIndex) == false))) {
					//System.out.println("This is the bit at current: " + remainder.getBit(currentIndex));
					currentIndex++;
				}
				//System.out.println("This is the current index: " + currentIndex);
				
				
		}
		
		//Remove extra 0's at the front
		//print("remainder before removing 0's", remainder);
		
		
		int zeroCount = 1;
		int index = 0;
		byte[] rem = remainder.toByteArray();
		//System.out.println("Byte: " + Arrays.toString(rem));
		while (index < rem.length -1 && rem[index + 1]==0) {
			zeroCount++;
			index++;
		}
		
		byte[] zero = new byte[1];
		zero[0] = 0;
		if (zeroCount == rem.length) return new BitVector(zero, 0,1);
		
		int start = zeroCount*8;
		
		BitVector newRemainder = new BitVector();
		int index2 = 0;
		for (int i = start; i < remainder.length(); i ++) {
			newRemainder.setBit(index2++, remainder.getBit(i));
		}
		
		//print("remainder after removing 0's", remainder);
		
		//print("remainder done", newRemainder);
		//System.out.println(Arrays.toString(newRemainder.toByteArray()));
		return newRemainder;   
	}

	// =========================================================================
	/**
	 * Determine whether the buffered data forms a complete frame.
	 * 
	 * @return Whether a complete buffer has arrived.
	 **/
	protected boolean receivedCompleteFrame() {

		// Any frame with less than two bytes cannot be complete, since even the
		// empty frame contains a start and a stop tag.
		if (bufferIndex < 2) {

			return false;

		}

		// A frame is complete iff the byte received is an non-escaped stop tag.
		return ((incomingBuffer[bufferIndex - 1] == _stopTag) && (incomingBuffer[bufferIndex - 2] != _escapeTag));

	} // receivedCompleteFrame
		// =========================================================================

	// =========================================================================
	/**
	 * Remove the framing metadata and return the original data.
	 * 
	 * @return The data carried in this frame; <tt>null</tt> if the data was not
	 *         successfully received.
	 **/
	protected byte[] processFrame() {

		// Allocate sufficient space to hold the original data, which
		// does not need space for the start/stop tags.
		byte[] originalData = new byte[bufferIndex - 2];

		// Check the start tag.
		int frameIndex = 0;
		if (incomingBuffer[frameIndex++] != _startTag) {

			System.err.println("ParityDLL: Missing start tag!");
			return null;

		}

		// Loop through the frame, extracting the bytes. Look ahead to find the
		// stop tag (making sure it is not escaped), because the byte before
		// that is the parity byte.
		int originalIndex = 0;
		while ((incomingBuffer[frameIndex + 1] != _stopTag)
				|| (incomingBuffer[frameIndex] == _escapeTag)) {

			// If the next original byte is escape-tagged, then skip
			// the tag so that only the real data is extracted.
			if (incomingBuffer[frameIndex] == _escapeTag) {

				frameIndex++;

			}

			// Copy the original byte.
			originalData[originalIndex++] = incomingBuffer[frameIndex++];

		}
		originalData[originalIndex++] = incomingBuffer[frameIndex++];

		// Allocate a space that is only as large as the original
		// message and then copy the original data into it.
		byte[] finalData = new byte[originalIndex];
		for (int i = 0; i < originalIndex; i++) {
			finalData[i] = originalData[i];
		}
		
		//System.out.println("Received message with remainder: " + Arrays.toString(finalData));
		
		BitVector messageWithRemainder = new BitVector(finalData,0,finalData.length);
		BitVector finalMessage = new BitVector();
		
		//Get rid of extra ending 0
		for (int i = 0; i < messageWithRemainder.length();i++) {
			finalMessage.setBit(i, messageWithRemainder.getBit(i));
		}
		
		
		//print("final message", finalMessage);

		// Calculate the remainder of the extracted data and make sure
		// it is 0. If it's not, return null.
		BitVector remainder = calculateRemainder(messageWithRemainder,false);
		boolean allZeroes = true;
		for (int i = 0; i < remainder.length(); i++) {
			if (remainder.getBit(i) != false) {
				allZeroes = false;
				break;
			}
		}
		//System.out.println(allZeroes);
		if (!allZeroes) {

			System.err.print("CRCDLL message: ");
			for (int i = 0; i < finalData.length; i++) {
				System.err.print((char) finalData[i]);
			}
			System.err.println(" <= Remainder not 0!");
			finalData = null;
			return finalData;

		}
		
		
		//print("remainder", remainder);
		
		//System.out.println("THIS IS THE FINAL DATA" + Arrays.toString(finalData));
		byte[] finalByteMessage = new byte[finalData.length-2];
		for (int i = 0; i < finalByteMessage.length; i++) {
			finalByteMessage[i] = finalData[i];
		}

		return finalByteMessage;

	} // processFrame
		// =========================================================================

	//======================================================================
	//Print BitVectors
	public static void print (String s, BitVector b) {
		System.out.println("This is the " + s);
		for (int i = 0; i < b.length(); i++) {
			if(b.getBit(i)) {
				System.out.print(1);
			} else {
				System.out.print(0);
			}
			if ((i+1) % 4 == 0)
				System.out.print(' ');
			
		} System.out.println();
	}
	
	
	// =========================================================================
	// DATA MEMBERS

	/**
	 * The tag that marks the beginning of a frame.
	 **/
	final byte _startTag = (byte) '{';

	/**
	 * The tag that marks the end of a frame.
	 **/
	final byte _stopTag = (byte) '}';

	/**
	 * The tag that marks the following byte as data (and not metadata).
	 **/
	final byte _escapeTag = (byte) '\\';

	/**
	 * The maximum number of data (not metadata) bytes in a frame.
	 **/
	final int _maxFrameSize = 8;

	byte[] _generatorByteArray = { (byte) 0xA6, (byte) 0xBC };

	private BitVector _generator = new BitVector(_generatorByteArray, 0,
			_generatorByteArray.length);
	
	
	// =========================================================================

	// =============================================================================
} // class CRCDataLinkLayer
// =============================================================================
