import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;
import java.io.IOException;

public class CheckExcelFile {
    public static void main(String[] args) {
        String filePath = "C:/Users/khoih/Downloads/products_with_barcode.xlsx";

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("File: " + filePath);
            System.out.println("Sheet name: " + sheet.getSheetName());
            System.out.println("Total rows: " + (sheet.getLastRowNum() + 1));

            // Read header row
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                System.out.println("\nColumns:");
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    if (cell != null) {
                        System.out.println((i + 1) + ". " + getCellValueAsString(cell));
                    }
                }
            }

            // Read sample data rows
            System.out.println("\nSample data (first 3 rows):");
            for (int i = 1; i <= Math.min(3, sheet.getLastRowNum()); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    System.out.println("Row " + i + ":");
                    for (int j = 0; j < row.getLastCellNum(); j++) {
                        Cell cell = row.getCell(j);
                        if (cell != null) {
                            System.out.println("  Col " + (j + 1) + ": " + getCellValueAsString(cell));
                        }
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
