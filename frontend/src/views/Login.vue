<template>
  <div class="login-page">
    <div class="login-brand">
      <div class="brand-mark">
        <el-icon :size="34"><Connection /></el-icon>
      </div>
      <h1 class="brand-name">知联汇学</h1>
      <p class="brand-sub">交叉学科知识图谱平台</p>
      <p class="brand-tagline">连接学科 · 启发思维 · 探索未来</p>
    </div>

    <div class="login-card glass-card">
      <div class="card-head">
        <span class="title">欢迎回来</span>
        <span class="subtitle">AI · 知识网络 · 跨学科学习</span>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入账号" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" placeholder="请输入密码" prefix-icon="Lock" size="large" type="password" @keyup.enter="handleLogin" />
            </el-form-item>
            <el-button class="submit-btn" type="primary" size="large" :loading="loading" @click="handleLogin" style="width:100%">
              登 录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="请输入账号" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" placeholder="请输入密码(6位以上)" prefix-icon="Lock" size="large" type="password" />
            </el-form-item>
            <el-form-item prop="realName">
              <el-input v-model="registerForm.realName" placeholder="请输入姓名" prefix-icon="Postcard" size="large" />
            </el-form-item>
            <el-form-item prop="role">
              <el-radio-group v-model="registerForm.role" size="large" class="role-group">
                <el-radio-button label="STUDENT">学生</el-radio-button>
                <el-radio-button label="TEACHER">教师</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-button class="submit-btn" type="primary" size="large" :loading="loading" @click="handleRegister" style="width:100%">
              注 册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login, register } from '@/api/user'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('login')
const loading = ref(false)

const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', realName: '', role: 'STUDENT' })

const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}
const registerRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少6位', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

async function handleLogin() {
  await loginFormRef.value.validate()
  loading.value = true
  try {
    const res = await login(loginForm)
    userStore.setToken(res.data.token)
    userStore.setUser({
      userId: res.data.userId,
      username: res.data.username,
      realName: res.data.realName,
      role: res.data.role
    })
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await registerFormRef.value.validate()
  loading.value = true
  try {
    await register(registerForm)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
  } catch (e) {
    // handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 90px;
  background: var(--bg-page);
  overflow: hidden;
}

.login-brand {
  text-align: center;
  max-width: 340px;

  .brand-mark {
    width: 84px;
    height: 84px;
    margin: 0 auto 18px;
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #1F2329;
    color: #fff;
    box-shadow: 0 8px 24px rgba(31, 35, 41, 0.15);
  }

  .brand-name {
    font-size: 36px;
    font-weight: 700;
    letter-spacing: 6px;
    color: var(--text-primary);
  }

  .brand-sub {
    margin-top: 10px;
    font-size: 16px;
    color: var(--text-secondary);
    letter-spacing: 2px;
  }

  .brand-tagline {
    margin-top: 24px;
    font-size: 13px;
    color: var(--text-muted);
    letter-spacing: 3px;
  }
}

.login-card {
  width: 400px;
  padding: 32px 36px;
  background: var(--bg-panel);
  border: 1px solid var(--border);
  border-radius: 12px;
}

.card-head {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 18px;

  .title { font-size: 20px; font-weight: 600; color: var(--text-primary); }
  .subtitle { margin-top: 6px; font-size: 12px; color: var(--text-muted); letter-spacing: 2px; }
}

.login-tabs {
  :deep(.el-tabs__nav-wrap::after) { background: var(--border); }
  :deep(.el-tabs__item) {
    font-size: 15px;
    color: var(--text-secondary);
    &:hover { color: var(--text-primary); }
  }
}

.role-group {
  display: flex;
  width: 100%;
  :deep(.el-radio-button__inner) {
    background: #FFFFFF;
    border-color: var(--border-strong);
    color: var(--text-secondary);
  }
  :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
    background: var(--primary);
    border-color: var(--primary);
    color: #fff;
  }
}

.submit-btn {
  margin-top: 6px;
  height: 44px;
  font-size: 16px;
  letter-spacing: 6px;
  border-radius: 8px;
}
</style>
