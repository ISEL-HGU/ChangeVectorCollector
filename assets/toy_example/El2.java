import java.io.FileInputStream;

public class El2
{
    public static void main( String[ ] args )
    {
        // 바이트 단위로 파일읽기
        String filePath = "src/ex0801/io/FileExam.java"; // 대상 파일
        FileInputStream fileStream = null; // 파일 스트림

        try
        {
            fileStream = new FileInputStream( filePath );// 파일 스트림 생성

            //파일 내용을 담을 버퍼(?) 선언
            byte[ ] readBuffer = new byte[ fileStream.available( ) ];
            while (fileStream.read( readBuffer ) != -1) //
            {
            }

            System.out.println( new String( readBuffer ) ); //출력

        }
        catch ( Exception e )
        {
            System.out.println( "파일 입출력 에러!!" + e );
        }
        finally
        {
            try
            {
                fileStream.close( );//파일 닫기. 여기에도 try/catch가 필요하다.
            }
            catch ( Exception e )
            {
                System.out.println( "닫기 실패" + e );
            }
        }
    }
}