package com.smartretail.serviceproduct.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ExcelFileGenerator {

    public static void main(String[] args) {
        try {
            generateProductImportTemplate();
            System.out.println("Đã tạo file Product_Import_Template.xlsx thành công!");
        } catch (IOException e) {
            System.err.println("Lỗi khi tạo file Excel: " + e.getMessage());
        }
    }

    public static void generateProductImportTemplate() throws IOException {
        Workbook workbook = new XSSFWorkbook();

        // Tạo sheet chính
        Sheet mainSheet = workbook.createSheet("Product Import Template");
        createMainSheet(mainSheet);

        // Tạo sheet hướng dẫn
        Sheet instructionSheet = workbook.createSheet("Hướng dẫn");
        createInstructionSheet(instructionSheet);

        // Lưu file
        try (FileOutputStream fileOut = new FileOutputStream("Product_Import_Template.xlsx")) {
            workbook.write(fileOut);
        }

        workbook.close();
    }

    private static void createMainSheet(Sheet sheet) {
        // Tạo style cho header
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle dataStyle = createDataStyle(sheet.getWorkbook());

        // Tạo header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "Name", "Description", "Category Name", "Unit Name",
            "Expiration Date", "Image URL"
        };

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Dữ liệu mẫu
        String[][] sampleData = {
            {
                "Coca Cola 330ml",
                "Nước ngọt Coca Cola lon 330ml",
                "Đồ uống",
                "Lon",
                "2025-12-31",
                "https://example.com/coca-cola.jpg"
            },
            {
                "Pepsi 330ml",
                "Nước ngọt Pepsi lon 330ml",
                "Đồ uống",
                "Lon",
                "2025-12-31",
                "https://example.com/pepsi.jpg"
            },
            {
                "Mì tôm Hảo Hảo",
                "Mì tôm Hảo Hảo gói 75g",
                "Thực phẩm",
                "Gói",
                "2025-06-30",
                "https://example.com/mi-tom.jpg"
            },
            {
                "Sữa tươi Vinamilk",
                "Sữa tươi Vinamilk hộp 1L",
                "Sữa",
                "Hộp",
                "2025-03-15",
                "https://example.com/sua-tuoi.jpg"
            },
            {
                "Bánh mì Kinh Đô",
                "Bánh mì Kinh Đô gói 500g",
                "Thực phẩm",
                "Gói",
                "2025-02-28",
                "https://example.com/banh-mi.jpg"
            },
            {
                "Nước suối Aquafina",
                "Nước suối Aquafina chai 500ml",
                "Đồ uống",
                "Chai",
                "2025-12-31",
                "https://example.com/nuoc-suoi.jpg"
            },
            {
                "Kẹo Chupa Chups",
                "Kẹo mút Chupa Chups hộp 20 viên",
                "Kẹo bánh",
                "Hộp",
                "2025-08-15",
                "https://example.com/keo-chupa.jpg"
            },
            {
                "Bánh Oreo",
                "Bánh quy Oreo gói 300g",
                "Kẹo bánh",
                "Gói",
                "2025-05-20",
                "https://example.com/oreo.jpg"
            },
            {
                "Nước mắm Nam Ngư",
                "Nước mắm Nam Ngư chai 500ml",
                "Gia vị",
                "Chai",
                "2026-01-10",
                "https://example.com/nuoc-mam.jpg"
            },
            {
                "Dầu ăn Neptune",
                "Dầu ăn Neptune chai 1L",
                "Gia vị",
                "Chai",
                "2025-11-30",
                "https://example.com/dau-an.jpg"
            }
        };

        // Thêm dữ liệu mẫu
        for (int i = 0; i < sampleData.length; i++) {
            Row dataRow = sheet.createRow(i + 1);
            for (int j = 0; j < sampleData[i].length; j++) {
                Cell cell = dataRow.createCell(j);
                cell.setCellValue(sampleData[i][j]);
                cell.setCellStyle(dataStyle);
            }
        }

        // Tự động điều chỉnh độ rộng cột
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void createInstructionSheet(Sheet sheet) {
        CellStyle headerStyle = createHeaderStyle(sheet.getWorkbook());
        CellStyle dataStyle = createDataStyle(sheet.getWorkbook());

        // Header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Cột", "Mô tả", "Ví dụ"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Dữ liệu hướng dẫn
        String[][] instructions = {
            {"Name", "Tên sản phẩm (BẮT BUỘC)", "Coca Cola 330ml"},
            {"Description", "Mô tả sản phẩm (TÙY CHỌN)", "Nước ngọt Coca Cola lon 330ml"},
            {"Category Name", "Tên danh mục (phải tồn tại trong hệ thống)", "Đồ uống"},
            {"Unit Name", "Tên đơn vị tính (phải tồn tại trong hệ thống)", "Lon"},
            {"Expiration Date", "Ngày hết hạn (format: YYYY-MM-DD)", "2025-12-31"},
            {"Image URL", "Đường dẫn ảnh sản phẩm (TÙY CHỌN)", "https://example.com/coca-cola.jpg"}
        };

        for (int i = 0; i < instructions.length; i++) {
            Row dataRow = sheet.createRow(i + 1);
            for (int j = 0; j < instructions[i].length; j++) {
                Cell cell = dataRow.createCell(j);
                cell.setCellValue(instructions[i][j]);
                cell.setCellStyle(dataStyle);
            }
        }

        // Tự động điều chỉnh độ rộng cột
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }
}
