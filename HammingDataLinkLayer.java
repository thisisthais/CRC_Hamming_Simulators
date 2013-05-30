import java.util.ArrayList;
import java.util.Arrays;

// =============================================================================
/**
 * A data link layer that colorless green ideas sleep furiously
 **/
public class HammingDataLinkLayer extends DataLinkLayer {
	// =============================================================================

	// =========================================================================
	/**
	 * The constructor. Make a new parity-checking data link layer.
	 * 
	 * @param physicalLayer
	 *            The physical layer through which this data link layer should
	 *            communicate.
	 **/
	public HammingDataLinkLayer(PhysicalLayer physicalLayer) {

		// Initialize the layer.
		initialize(physicalLayer);

	} // HammingDataLinkLayer
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
		byte[] framedData = new byte[(_maxFrameSize * 2) + 3];

		// Begin with the start tag.
		int frameIndex = 0;
		framedData[frameIndex++] = _startTag;
		
		
		// Hamming-fy the data
		BitVector plainMessage = new BitVector(data,begin,end);
		//print("plain message: ", plainMessage);
		
		
		BitVector hammingData = createHamming(plainMessage);
		
		//Pad with 0's
		
		
		data = hammingData.toByteArray();
		
		//print("Hamming Data: ", hammingData);
		//System.out.println(Arrays.toString(data));

		// Add each byte of original data.
		for (int dataIndex = 0; dataIndex < data.length; dataIndex++) {

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


		// End with a stop tag.
		framedData[frameIndex++] = _stopTag;

		// Copy the complete frame into a buffer of the exact desired
		// size.
		byte[] finalFrame = new byte[frameIndex];
		for (int i = 0; i < frameIndex; i++) {
			finalFrame[i] = framedData[i];
		}

		//System.out.println("Final Frame sent: " + Arrays.toString(finalFrame));
		return finalFrame;

	} // constructFrame (byte[] data, int begin, int end)
		// =========================================================================

	public static BitVector createHamming(BitVector data) {

		BitVector hamming = new BitVector();

		// Make space for parity bits at power of 2 indices by placing 0 bit
		int dataIndex = 0;
		int hammingIndex = 1;

		while (dataIndex < data.length()) {
			if (isPowerOf2(hammingIndex)) {
				hamming.setBit(hammingIndex++, false);
			} else {
				hamming.setBit(hammingIndex - 1, data.getBit(dataIndex++));
				hammingIndex++;
			}
		}

		//print("Hamming after spaces: ", hamming);

		hammingIndex = 1;
		while (hammingIndex < hamming.length()) {
			if (isPowerOf2(hammingIndex)) {
				//System.out.println("Parity at " + hammingIndex + " is " + (calculateParity(hammingIndex - 1, hamming)));
				hamming.setBit(hammingIndex - 1, calculateParity(hammingIndex - 1, hamming));
			}
			hammingIndex++;
		}

		return hamming;

	}

	public static boolean calculateParity(int index, BitVector data) {
		boolean parity = data.getBit(index);

		for (int i = index; i < data.length(); i++) {
			if (((i + 1) & (index + 1)) != 0) {
				//System.out.println("Checking " + (i+1) + " and " + (index+1 + " gets " + (parity ^ data.getBit(i))));
				parity = parity ^ data.getBit(i);
			}
		}
		
		//System.out.println("Calculated parity at " + (index+1) + ": " + parity);
		return parity;
	}
	
	public static ArrayList<Integer> verifyHamming(BitVector data) {
		ArrayList<Integer> wrongIndices = new ArrayList<Integer>();
		
		int checkIndex = 1;
		while (checkIndex < data.length()) {
			if (isPowerOf2(checkIndex)) {
				boolean parity = calculateParity(checkIndex-1,data);
				if (parity != data.getBit(checkIndex-1)) {
					//Note that you are adding indices starting from 1
					wrongIndices.add(new Integer(checkIndex));
				}
			}
			
			checkIndex++;
		}
		
		//System.out.println(wrongIndices.toString());
		return wrongIndices;
	}
	
	public static BitVector correctError (BitVector data, ArrayList<Integer> wrongIndices) {
		
		// System.out.println("Begin correction.");
		// System.out.println(wrongIndices.toString());

		int sum = 0;
		for (int i = 0; i < wrongIndices.size(); i++) {
			sum += wrongIndices.get(i);
		}

		// System.out.println("This is the sum: " + sum);

		if (sum!=0) data.setBit(sum - 1, !data.getBit(sum - 1));

		return data;
	}

	public static boolean isPowerOf2(int hammingIndex) {
		return 2 * hammingIndex == (hammingIndex ^ (hammingIndex - 1)) + 1;
	}

	public static void print(String s, BitVector b) {
		System.out.println("This is the " + s);
		for (int i = 0; i < b.length(); i++) {
			if (b.getBit(i)) {
				System.out.print(1);
			} else {
				System.out.print(0);
			}
			if ((i + 1) % 4 == 0)
				System.out.print(' ');

		}
		System.out.println();
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
		// hamming message and then copy the original data into it.
		byte[] finalData = new byte[originalIndex];
		for (int i = 0; i < originalIndex; i++) {
			finalData[i] = originalData[i];
		}
		
		//System.out.println("Final Data received: " + Arrays.toString(finalData));

		// Verify that the hamming message is correct. Store any indices whose
		// parity is wrong. Find the wrong index and correct it.
		BitVector hammingMessage = new BitVector(finalData,0,finalData.length);
		//print("Received hamming message: ", hammingMessage);
		
		//Deconstruct the hamming data into the message before correction
		BitVector finalWrongMessage = new BitVector();
		int hammingIndex1 = 1;
		int finalMessageIndex1 = 0;
		while (hammingIndex1 < hammingMessage.length() + 1) {
			if (isPowerOf2(hammingIndex1)) {
				hammingIndex1++;
			} else {
				finalWrongMessage.setBit(finalMessageIndex1++,hammingMessage.getBit(hammingIndex1-1));
				hammingIndex1++;
			}
		}
		byte[] wrongMessage = finalWrongMessage.toByteArray();
		
		
		ArrayList<Integer> wrongIndices = verifyHamming(hammingMessage);
		BitVector correctMessage = correctError(hammingMessage,wrongIndices);
		
		//System.out.println("Corrected message " + Arrays.toString(correctMessage.toByteArray()));
		
		//Deconstruct the hamming data into the message after correction
				BitVector finalMessage = new BitVector();
				int hammingIndex = 1;
				int finalMessageIndex = 0;
				while (hammingIndex < correctMessage.length() + 1) {
					if (isPowerOf2(hammingIndex)) {
						hammingIndex++;
					} else {
						finalMessage.setBit(finalMessageIndex++,correctMessage.getBit(hammingIndex-1));
						hammingIndex++;
					}
				}
				
		finalData = finalMessage.toByteArray();
		
		if (wrongIndices.size() != 0) {

			//used to be finaldata
			System.err.print("ParityDLL message: ");
			for (int i = 0; i < wrongMessage.length; i++) {
				System.err.print((char) wrongMessage[i]);
			}
			System.err.println(" <= Parity mismatch! Wrong indices " + wrongIndices.toString());
			

		}
		
		
		//print("final message with hamming: ", correctMessage);
		//print("final message w/0 hamming: ", finalMessage);
		//System.out.println("FINAL FINAL messgae: " + Arrays.toString(finalData));
		return finalData;

	} // processFrame
		// =========================================================================

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
	// =========================================================================

	// =============================================================================
} // class HammingDataLinkLayer
// =============================================================================
