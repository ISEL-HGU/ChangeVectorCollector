import java.io.FileInputStream;
import java.io.FileOutputStream;

public class El1
{
	public static void main(String[] args)
	{
		// 바이트 단위로 파일읽기
		String filePath = "src/ex0801/io/aaa.zip"; // 대상 파일
		FileInputStream inputStream = null; // 파일 읽기 스트림
		FileOutputStream outputStream = null; // 파일 쓰기 스트림

		try
		{
			inputStream = new FileInputStream(filePath);// 파일 입력 스트림 생성
			outputStream = new FileOutputStream("src/ex0801/io/bb.zip");// 파일 출력 스트림 생성

			// 파일 내용을 담을 버퍼(?) 선언
			byte[] readBuffer = new byte[1024];
			while (inputStream.read(readBuffer, 0, readBuffer.length) != -1)
			{
				//버퍼 크기만큼 읽을 때마다 출력 스트림에 써준다.
				outputStream.write(readBuffer);
			}

		}
		catch (Exception e)
		{
			System.out.println("파일 입출력 에러!!" + e);
		}
		finally
		{
			try
			{
				// 파일 닫기. 여기에도 try/catch가 필요하다.
				inputStream.close();
				outputStream.close();
			}
			catch (Exception e)
			{
				System.out.println("닫기 실패" + e);
			}
		}
	}
}