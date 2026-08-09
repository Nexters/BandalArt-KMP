/*
 * Copyright 2026 easyhooon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nexters.bandalart.core.domain.template

enum class BandalartTemplateId(
    val storedValue: String,
) {
    JOB_PREPARATION_V1("job_preparation_v1"),
    WORKOUT_HABIT_V1("workout_habit_v1"),
    STUDY_PLAN_V1("study_plan_v1"),
    MONEY_HABIT_V1("money_habit_v1"),
    TRAVEL_PLAN_V1("travel_plan_v1"),
    ;

    companion object {
        fun fromStoredValue(value: String?): BandalartTemplateId? = entries.firstOrNull { it.storedValue == value }
    }
}

data class BandalartTemplate(
    val id: BandalartTemplateId,
    val title: String,
    val summary: String,
    val profileEmoji: String,
    val subGoals: List<BandalartTemplateSubGoal>,
)

data class BandalartTemplateSubGoal(
    val title: String,
    val tasks: List<String>,
)

object BandalartTemplateCatalog {
    val templates: List<BandalartTemplate> =
        listOf(
            BandalartTemplate(
                id = BandalartTemplateId.JOB_PREPARATION_V1,
                title = "취업 준비",
                summary = "지원 준비부터 면접까지",
                profileEmoji = "💼",
                subGoals =
                    listOf(
                        subGoal("지원 전략", "희망 직무 정하기", "기업 목록 만들기", "지원 일정 정리하기"),
                        subGoal("서류 준비", "이력서 업데이트", "자기소개서 초안", "포트폴리오 점검"),
                        subGoal("실력 준비", "핵심 역량 공부", "예상 과제 연습", "경험 사례 정리"),
                        subGoal("면접 준비", "예상 질문 답변", "모의 면접", "회사별 질문 준비"),
                    ),
            ),
            BandalartTemplate(
                id = BandalartTemplateId.WORKOUT_HABIT_V1,
                title = "운동 습관",
                summary = "운동·회복·기록을 꾸준하게",
                profileEmoji = "💪",
                subGoals =
                    listOf(
                        subGoal("운동 계획", "주간 횟수 정하기", "운동 시간 확보", "운동복 미리 준비"),
                        subGoal("근력 운동", "하체 운동", "상체 운동", "코어 운동"),
                        subGoal("유산소", "걷기 또는 달리기", "심박수 기록", "주간 거리 확인"),
                        subGoal("회복 습관", "스트레칭", "수면 시간 지키기", "몸 상태 기록"),
                    ),
            ),
            BandalartTemplate(
                id = BandalartTemplateId.STUDY_PLAN_V1,
                title = "공부 계획",
                summary = "목표부터 복습·실전까지",
                profileEmoji = "📚",
                subGoals =
                    listOf(
                        subGoal("목표 설계", "시험일 확인", "학습 범위 나누기", "주간 목표 정하기"),
                        subGoal("개념 학습", "교재 1회독", "핵심 개념 정리", "모르는 내용 표시"),
                        subGoal("복습", "오답 노트", "주간 복습", "암기 내용 점검"),
                        subGoal("실전 연습", "기출 문제 풀기", "시간 재고 풀기", "약점 보완"),
                    ),
            ),
            BandalartTemplate(
                id = BandalartTemplateId.MONEY_HABIT_V1,
                title = "재테크 습관",
                summary = "예산·저축·투자를 한눈에",
                profileEmoji = "💰",
                subGoals =
                    listOf(
                        subGoal("예산 관리", "월 예산 정하기", "고정비 확인", "주간 지출 기록"),
                        subGoal("저축", "저축 목표 정하기", "자동 이체 설정", "비상금 만들기"),
                        subGoal("투자 공부", "투자 원칙 정리", "관심 자산 공부", "위험 수준 점검"),
                        subGoal("정기 점검", "월말 결산", "자산 현황 기록", "다음 달 계획 수정"),
                    ),
            ),
            BandalartTemplate(
                id = BandalartTemplateId.TRAVEL_PLAN_V1,
                title = "여행 준비",
                summary = "일정·예약·짐·현지 준비",
                profileEmoji = "🧳",
                subGoals =
                    listOf(
                        subGoal("일정 짜기", "여행 기간 정하기", "가고 싶은 곳 저장", "동선 정리"),
                        subGoal("예약", "교통편 예약", "숙소 예약", "입장권 확인"),
                        subGoal("짐 챙기기", "필수 서류", "의류와 세면도구", "충전기와 어댑터"),
                        subGoal("현지 준비", "예산과 환전", "교통 방법 확인", "비상 연락처 저장"),
                    ),
            ),
        )

    fun find(id: BandalartTemplateId): BandalartTemplate = templates.first { it.id == id }
}

private fun subGoal(
    title: String,
    vararg tasks: String,
): BandalartTemplateSubGoal =
    BandalartTemplateSubGoal(
        title = title,
        tasks = tasks.toList(),
    )
