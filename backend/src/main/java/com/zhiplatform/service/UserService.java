package com.zhiplatform.service;

import com.zhiplatform.dto.LoginRequest;
import com.zhiplatform.dto.LoginResponse;
import com.zhiplatform.dto.RegisterRequest;
import com.zhiplatform.entity.mysql.Clazz;
import com.zhiplatform.entity.mysql.User;
import com.zhiplatform.repository.mysql.ClazzRepository;
import com.zhiplatform.repository.mysql.UserRepository;
import com.zhiplatform.security.JwtUtil;
import com.zhiplatform.common.Result;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final ClazzRepository clazzRepository;
    private final TeachingService teachingService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager,
                       ClazzRepository clazzRepository, TeachingService teachingService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.clazzRepository = clazzRepository;
        this.teachingService = teachingService;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (org.springframework.security.authentication.DisabledException e) {
            // 待审核 / 禁用账号：单独抛出以便控制器给出明确提示
            throw e;
        }
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        return LoginResponse.builder()
                .token(token)
                .role(user.getRole())
                .username(user.getUsername())
                .realName(user.getRealName())
                .userId(user.getId())
                .build();
    }

    public Result<Void> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return Result.error(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setClassName(request.getClassName());
        user.setSchool(request.getSchool());
        boolean isTeacher = "TEACHER".equalsIgnoreCase(request.getRole());
        user.setRole(isTeacher ? "TEACHER" : "STUDENT");
        // 教师注册需管理员审核（status=2 待审核，未通过无法登录）；学生直接可用
        user.setStatus(isTeacher ? 2 : 1);
        userRepository.save(user);
        Result<Void> result = Result.success();
        result.setMessage(isTeacher ? "注册成功，教师账号需管理员审核通过后方可登录" : "注册成功，请登录");
        return result;
    }

    /**
     * 管理员新增账号（教师/学生）。
     */
    public Result<User> addUser(String username, String password, String realName, String role) {
        if (username == null || username.isBlank() || password == null || password.length() < 6) {
            return Result.error(400, "账号或密码不合法（密码至少 6 位）");
        }
        if (!"TEACHER".equalsIgnoreCase(role) && !"STUDENT".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            return Result.error(400, "角色不合法");
        }
        if (userRepository.existsByUsername(username)) {
            return Result.error(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role.toUpperCase());
        user.setRealName(realName);
        user.setStatus(1);
        userRepository.save(user);
        return Result.success(user);
    }

    /**
     * 用户修改自己的密码。
     */
    public Result<Void> changePassword(String username, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error(400, "新密码至少 6 位");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error(400, "原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return Result.success();
    }

    /**
     * 管理员重置密码。
     */
    public Result<Void> resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            return Result.error(400, "新密码至少 6 位");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return Result.success();
    }
    /**
     * 管理员修改用户身份（角色）。
     */
    public Result<Void> updateUserRole(Long userId, String role) {
        if (role == null || (!"ADMIN".equalsIgnoreCase(role)
                && !"TEACHER".equalsIgnoreCase(role) && !"STUDENT".equalsIgnoreCase(role))) {
            return Result.error(400, "角色不合法");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if ("admin".equals(user.getUsername())) {
            return Result.error(400, "内置管理员账号不可修改身份");
        }
        user.setRole(role.toUpperCase());
        userRepository.save(user);
        return Result.success();
    }

    /**
     * 管理员删除账号（如拒绝教师注册申请）。
     */
    public Result<Void> deleteUser(Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return Result.success();
        if ("admin".equals(user.getUsername())) {
            return Result.error(400, "内置管理员账号不可删除");
        }
        userRepository.deleteById(id);
        return Result.success();
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void updateUserStatus(Long userId, Integer status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setStatus(status);
        userRepository.save(user);
    }

    /**
     * Excel 批量导入学生账号：
     * 列 0：用户名（必填，已存在则跳过并计入失败明细）；列 1：姓名；
     * 列 2：初始密码（可选，默认 123456）；列 3：班级名（可选，班级存在则加入班级成员，
     * 班级不存在时仅创建账号并在备注明细中注明）。
     * 返回成功数、失败明细与备注明细。
     */
    public Map<String, Object> batchImportStudents(MultipartFile file) throws IOException {
        List<Map<String, Object>> failures = new ArrayList<>();
        List<Map<String, Object>> notes = new ArrayList<>();
        int success = 0;
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String username = getCellString(row.getCell(0));
                if (username == null || username.isBlank()) continue;
                String realName = getCellString(row.getCell(1));
                String password = getCellString(row.getCell(2));
                String className = getCellString(row.getCell(3));
                if (password == null || password.isBlank()) {
                    password = "123456";
                }

                if (userRepository.existsByUsername(username)) {
                    failures.add(importDetail(i + 1, username, "用户名已存在"));
                    continue;
                }
                if (password.length() < 6) {
                    failures.add(importDetail(i + 1, username, "密码至少 6 位"));
                    continue;
                }

                User user = new User();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode(password));
                user.setRealName(realName);
                user.setRole("STUDENT");
                user.setStatus(1);
                userRepository.save(user);
                success++;

                // 可选：按班级名加入班级（复用 TeachingService 加成员逻辑，保持图数据同步）
                if (className != null && !className.isBlank()) {
                    Clazz clazz = clazzRepository.findByName(className.trim())
                            .stream().findFirst().orElse(null);
                    if (clazz == null) {
                        notes.add(importDetail(i + 1, username,
                                "班级「" + className.trim() + "」不存在，仅创建账号"));
                    } else {
                        Result<Void> r = teachingService.addClassMember(clazz.getId(), user.getId());
                        if (r.getCode() != 200) {
                            notes.add(importDetail(i + 1, username,
                                    "账号已创建，加入班级失败：" + r.getMessage()));
                        }
                    }
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", success);
        result.put("failCount", failures.size());
        result.put("failures", failures);
        result.put("notes", notes);
        return result;
    }

    private Map<String, Object> importDetail(int row, String username, String reason) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("row", row);
        detail.put("username", username);
        detail.put("reason", reason);
        return detail;
    }

    // 单元格取值与知识点导入保持一致：数字按文本读取（避免学号类用户名变科学计数），公式读缓存结果
    private String getCellString(Cell cell) {
        if (cell == null) return "";
        CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> NumberToTextConverter.toText(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}



