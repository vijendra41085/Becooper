package com.example.domain

data class DeliverableSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val codeOrSpec: String
)

object ArchitectureDeliverablesData {
    val deliverables: List<DeliverableSection> = listOf(
        DeliverableSection(
            id = "ERD_ARCH",
            title = "1. System Architecture & ERD Schema",
            subtitle = "Multi-tier Garment ERP Architecture + Entity-Relationship Diagram (Users, RBAC, Karigars, Inventory, Job Lots, Process Challans, QC Checkpoints, Double-Entry Ledger, Shiprocket/Razorpay/NIC E-Invoice)",
            badge = "ARCHITECTURE",
            codeOrSpec = """
┌────────────────────────────────────────────────────────────────────────────┐
│                  C B CREATIONS — GARMENT ERP ARCHITECTURE                  │
├────────────────────────────────────────────────────────────────────────────┤
│ [Client Layer]                                                             │
│  • Android Jetpack Compose App (Offline-First Room DB + Material 3 UI)     │
│  • Web ERP Portal (Next.js 15 App Router, Tailwind CSS, Shadcn UI)         │
├────────────────────────────────────────────────────────────────────────────┤
│ [RBAC Security & Audit Middleware]                                         │
│  • Roles: Administrator | Production Manager | Inventory Clerk |           │
│           Karigar (Worker) | Brand Representative                          │
│  • Immutable Audit Logger: Captures Before/After JSON on every Edit/Delete │
├────────────────────────────────────────────────────────────────────────────┤
│ [Core Domain Engines]                                                      │
│  1. Material & Lot Inventory Engine (Fabric, Katha Thread by Color, Trims) │
│  2. Karigar Rate Master, Punch-In/Out Timer & Running Balance Ledger       │
│  3. Speed Booster & Gamification Engine (+12% / +10% Early 0-Defect Bonus) │
│  4. Multi-Stage Quality Control (QC) Engine (Post-Stitch/Katha/Dhaga/Final)│
│  5. 8-Stage Real-Time Job Costing & Double-Entry Accounting Bookkeeping    │
├────────────────────────────────────────────────────────────────────────────┤
│ [External Cloud Integrations]                                              │
│  • Gemini Flash API (Multimodal Challan OCR + Bottleneck & Schedule AI)    │
│  • Meta WhatsApp Cloud API (Hindi Templates A: Stitching, B: Dhaga, C:Katha│
│  • Razorpay API (Payment Links + Webhook Ledger Auto-Settlement)           │
│  • Shiprocket API (Order Sync, AWB Generation, Live Courier Tracking)      │
│  • GST NIC E-Invoice & E-Way Bill API (IRN Hash + Signed QR Code v1.1)     │
└────────────────────────────────────────────────────────────────────────────┘

[ENTITY RELATIONSHIP DIAGRAM (ERD)]
Users (1) ──────< UserRoles >────── (1) RbacPermissions [role, module, CRUD+WA]
  │
  └───< AuditLogs [module, recordRef, previousJson, newJson, performedByRole]

JobOrders / Lots (1) ──┬──< MaterialIssues [fabric, kathaThreadColor, labels, buttons]
                       ├──< ProcessChallans [S..5XL matrix, cuttingPcs, ratePerPc, stage]
                       ├──< QcInspections [stage, inspectedPcs, defectivePcs, reason, action, status]
                       ├──< JobCostSheets [fabric+cut+katha+emb+stitch+dhaga+press+pack+freight]
                       └──< Shipments & EInvoices [shiprocketAwb, irnHash, signedQrCode]

Karigars (1) ──────────┬──< ProcessChallans [assigned Karigar, targetHours, actualHours, speedBonus]
                       ├──< KarigarAttendanceLogs [lotNo, punchInAt, punchOutAt, minsPerPc]
                       └──< AccountingVouchers [double-entry Debit/Credit ledger & Razorpay payouts]
            """.trimIndent()
        ),
        DeliverableSection(
            id = "PRISMA_SCHEMA",
            title = "2. Complete Prisma Schema (schema.prisma)",
            subtitle = "Production PostgreSQL Prisma ORM Schema covering RBAC, Inventory, Karigars, Size-Wise Challans, QC Checkpoints, Double-Entry Accounting, and Audit Logs",
            badge = "PRISMA / SQL",
            codeOrSpec = """
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

generator client {
  provider = "prisma-client-js"
}

enum RoleType {
  ADMINISTRATOR
  PRODUCTION_MANAGER
  INVENTORY_CLERK
  KARIGAR_WORKER
  BRAND_REPRESENTATIVE
}

enum KarigarCategory {
  FABRIC_CUTTER
  FABRICATOR_STITCHING
  KATHA_WORKER
  EMBROIDERY_WORKER
  DHAGA_CUTTER
  PRESSMAN
  PACKER
}

enum ChallanType {
  STITCHING_ISSUE
  DHAGA_ISSUE
  KATHA_ISSUE
  EMBROIDERY_ISSUE
  PURCHASE_CHALLAN
  DELIVERY_CHALLAN
}

enum QcStage {
  POST_CUTTING
  POST_KATHA_WORK
  POST_EMBROIDERY
  POST_STITCHING
  POST_DHAGA_CUTTING
  FINAL_INSPECTION
}

enum QcStatus {
  PENDING
  PASSED
  REWORK_REQUIRED
  QUARANTINED
}

model RbacPermission {
  id                  String   @id @default(cuid())
  role                RoleType
  moduleName          String   // INVENTORY, JOB_WORK, PRODUCTION_QC, FINANCE, AI_BLUEPRINT
  canView             Boolean  @default(true)
  canCreate           Boolean  @default(false)
  canEdit             Boolean  @default(false)
  canDelete           Boolean  @default(false)
  canDispatchWhatsApp Boolean  @default(false)
  @@unique([role, moduleName])
}

model Karigar {
  id                 String          @id @default(cuid())
  name               String
  phone              String          @unique
  category           KarigarCategory
  rateAmount         Decimal         @db.Decimal(10, 2)
  rateUnit           String          // PER_PIECE, PER_METER, PER_PROCESS
  runningBalanceInr  Decimal         @default(0) @db.Decimal(12, 2)
  defectFreePercent  Float           @default(100)
  avgSpeedScore      Int             @default(90)
  earnedIncentiveInr Decimal         @default(0) @db.Decimal(12, 2)
  challans           ProcessChallan[]
  qcInspections      QcInspection[]
}

model ProcessChallan {
  id                  String      @id @default(cuid())
  challanNo           String      @unique
  challanType         ChallanType
  lotNo               String
  itemName            String
  karigarId           String
  karigar             Karigar     @relation(fields: [karigarId], references: [id])
  issueDate           DateTime    @default(now())
  targetDeadlineHours Int         @default(48)
  actualHoursTaken    Int?
  sizeS               Int         @default(0)
  sizeM               Int         @default(0)
  sizeL               Int         @default(0)
  sizeXl              Int         @default(0)
  sizeXxl             Int         @default(0)
  size3xl             Int         @default(0)
  size4xl             Int         @default(0)
  size5xl             Int         @default(0)
  totalPcs            Int
  cuttingPcs          Int
  kathaThreadColor    String
  labelPcs            Int
  ratePerPc           Decimal     @db.Decimal(10, 2)
  speedBonusInr       Decimal     @default(0) @db.Decimal(10, 2)
  qcStatus            QcStatus    @default(PENDING)
  workflowStage       String
  qcLogs              QcInspection[]
}

model QcInspection {
  id               String         @id @default(cuid())
  lotNo            String
  challanId        String
  challan          ProcessChallan @relation(fields: [challanId], references: [id])
  karigarId        String
  karigar          Karigar        @relation(fields: [karigarId], references: [id])
  stage            QcStage
  inspectedPcs     Int
  defectivePcs     Int
  defectReason     String
  severity         String         // MINOR, MAJOR, CRITICAL
  correctiveAction String
  qcDecision       QcStatus
  inspectorName    String
  inspectedAt      DateTime       @default(now())
}

model AccountingVoucher {
  id              String   @id @default(cuid())
  voucherNo       String   @unique
  voucherType     String   // DEBIT_PAYMENT, CREDIT_RECEIPT, JOB_WORK_BILL, GST_E_INVOICE
  partyName       String
  debitAccount    String
  creditAccount   String
  amountInr       Decimal  @db.Decimal(12, 2)
  gstRatePercent  Float    @default(5.0)
  lotNo           String?
  razorpayLinkId  String?
  shiprocketAwb   String?
  irnHash         String?
  createdAt       DateTime @default(now())
}

model AuditLog {
  id              String   @id @default(cuid())
  module          String
  recordRef       String
  actionType      String
  previousValue   Json
  newValue        Json
  performedByRole RoleType
  createdAt       DateTime @default(now())
}
            """.trimIndent()
        ),
        DeliverableSection(
            id = "REST_API_SPEC",
            title = "3. Full REST API Endpoint Specifications",
            subtitle = "REST Endpoints for Challans, QC Workflow Sync, RBAC Guard, Razorpay Webhooks, Shiprocket AWB Dispatch, and NIC GST E-Invoice Generation",
            badge = "REST API",
            codeOrSpec = """
# 1. Process Challans & Automated WhatsApp Dispatch
POST   /api/v1/challans/issue
  • Guards: RBAC(module="JOB_WORK", permission="CREATE")
  • Body: { challanType, lotNo, itemName, karigarId, sizes: {S..5XL}, cuttingPcs, kathaThreadColor, labelPcs, targetDeadlineHours }
  • Action: Deducts raw material stock, creates ProcessChallan, formats Hindi Template (A/B/C), dispatches Meta WhatsApp Cloud API message.

PATCH  /api/v1/challans/:id
  • Guards: RBAC(module="JOB_WORK", permission="EDIT")
  • Action: Updates historical challan fields, recalculates Karigar running balance, writes immutable AuditLog entry.

# 2. Quality Control (QC) Inspection & Workflow Sync
POST   /api/v1/qc/inspections
  • Guards: RBAC(module="PRODUCTION_QC", permission="CREATE")
  • Body: { lotNo, challanId, stage, inspectedPcs, defectivePcs, defectReason, severity, correctiveAction, qcDecision }
  • Action: Records QC inspection, updates JobLot workflowStage & qcStatus, triggers Speed Booster bonus if qcDecision == "PASSED" & defectivePcs == 0.

# 3. Role-Based Access Control (RBAC) Management
GET    /api/v1/rbac/permissions?role=PRODUCTION_MANAGER
PATCH  /api/v1/rbac/permissions/:id
  • Guards: RBAC(role="ADMINISTRATOR")
  • Action: Updates granular CRUD + WhatsApp permissions per role & module with audit trail.

# 4. Razorpay Payment Links & Webhook Settlement
POST   /api/v1/payments/razorpay/create-link
POST   /api/v1/webhooks/razorpay
  • Header: X-Razorpay-Signature (HMAC-SHA256 verified)
  • Event: "payment_link.paid" -> Auto-posts Double-Entry Credit Receipt Voucher & settles party ledger.

# 5. Shiprocket Logistics & NIC GST E-Invoice v1.1
POST   /api/v1/logistics/shiprocket/awb-sync
  • Body: { orderId, lotNo, courierId, dimensions, weightKg }
  • Returns: { awbCode, labelPdfUrl, trackingUrl }

POST   /api/v1/compliance/gst/e-invoice
  • Body: { voucherNo, buyerGstin, hsnCode: "620442", lotNo, totalPcs, taxableValue }
  • Returns: { irnHash, ackNo, ackDt, signedQrCodeJwt, ewayBillNo }
            """.trimIndent()
        ),
        DeliverableSection(
            id = "WHATSAPP_NODE_TS",
            title = "4. WhatsApp Dispatch Service (Node.js / TypeScript)",
            subtitle = "Meta WhatsApp Cloud API + Hindi Template Formatter for Stitching (A), Dhaga Cutting (B), and Katha Work (C)",
            badge = "TYPESCRIPT",
            codeOrSpec = """
// services/whatsappDispatchService.ts
import axios from 'axios';

export interface ChallanPayload {
  challanType: 'STITCHING_ISSUE' | 'DHAGA_ISSUE' | 'KATHA_ISSUE';
  date: string;
  karigarName: string;
  karigarPhone: string;
  challanNo: string;
  lotNo: string;
  itemName: string;
  sizes?: { S: number; M: number; L: number; XL: number; XXL: number; '3XL': number; '4XL': number; '5XL': number };
  totalPcs: number;
  cuttingPcs: number;
  kathaThreadColor: string;
  labelPcs: number;
}

export function buildCbCreationsHindiMessage(p: ChallanPayload): string {
  if (p.challanType === 'STITCHING_ISSUE') {
    const s = p.sizes ?? { S: 0, M: 0, L: 0, XL: 0, XXL: 0, '3XL': 0, '4XL': 0, '5XL': 0 };
    return `C B CREATIONS - कटिंग इशू
दिनांक: ${'$'}{p.date}
कारीगर का नाम: ${'$'}{p.karigarName}
चालान नं.: ${'$'}{p.challanNo}
आइटम: ${'$'}{p.itemName}
पीस विवरण (Size Wise):
• S: ${'$'}{s.S} | M: ${'$'}{s.M} | L: ${'$'}{s.L} | XL: ${'$'}{s.XL} | XXL: ${'$'}{s.XXL} | 3XL: ${'$'}{s['3XL']} | 4XL: ${'$'}{s['4XL']} | 5XL: ${'$'}{s['5XL']}
कुल पीस (Total Pcs): ${'$'}{p.totalPcs}
इशू सामान:
 * कटिंग पीस: ${'$'}{p.cuttingPcs} पीस
 * काथा धागा (कलर): ${'$'}{p.kathaThreadColor}
 * लेबल: ${'$'}{p.labelPcs} पीस

मुख्य सिलाई निर्देश:
• 1 इंच में 13 टाँके, लुब व डबल सिलाई
• फ्रंट पट्टी: 10 × 2.5 इंच | V-कट: 1.5 इंच
• कुर्ती लंबाई: 32 इंच | स्लीव: 17 इंच (1 इंच पट्टी + टाँकी)
• दोनों साइड चाक पट्टी
• बॉटम चेस्ट से 1.5 इंच प्लस, नीचे से कम से कम फोल्ड

⚠️ सूचना: कटिंग पीस, लेबल व सामान गिनकर ही लेकर जाएँ। ले जाने के बाद कोई ज़िम्मेदारी नहीं होगी।
Phone: 9829211122, 9509901475`;
  }

  if (p.challanType === 'DHAGA_ISSUE') {
    return `C B CREATIONS - धागा कटिंग इशू
दिनांक: ${'$'}{p.date}
कारीगर का नाम: ${'$'}{p.karigarName}
लॉट नं. / स्टाइल: ${'$'}{p.lotNo}
कुल पीस: ${'$'}{p.totalPcs}
ज़रूरी निर्देश:
 * सभी पीस से धागे की कटिंग बिल्कुल साफ़-सुथरी और बारीकी से करें।
 * कैंची से कपड़े या सिलाई को कोई नुकसान नहीं पहुँचना चाहिए।
 * काम पूरा करके पीस साफ़ मोड़कर जमा कराएं।
⚠️ सूचना: कृपया सभी पीस गिनकर ही लेकर जाएँ। बाद में किसी कमी की ज़िम्मेदारी नहीं होगी।
संपर्क: 9829211122, 9509901475`;
  }

  return `C B CREATIONS - काथा वर्क आवश्यक निर्देश
दिनांक: ${'$'}{p.date}
कारीगर का नाम: ${'$'}{p.karigarName}
लॉट नं.: ${'$'}{p.lotNo}
कुल पीस: ${'$'}{p.totalPcs}
धागा कलर: ${'$'}{p.kathaThreadColor} (जो धागा इशू किया गया है, उसी कलर से काथा वर्क करना है)
⚠️ ज़रूरी हिदायतें:
 * डिज़ाइन: काथा वर्क का जो डिज़ाइन दिया गया है, बिल्कुल उसी डिज़ाइन में काम करें। गलत होने पर दोबारा खुलवा कर करवाया जाएगा, माल स्वीकार नहीं किया जाएगा।
 * पीस की सुरक्षा: पीस का पूरा ध्यान रखें। किसी भी पीस पर कोई कैंची आदि न लगे और पीस कटे-फटे नहीं। यदि कोई पीस कटता या फटता है, तो पूरी ज़िम्मेदारी आपकी होगी।
 * धागा: जो धागा इशू किया गया है, उसी कलर से काथा वर्क करना है।
नोट: कृपया सभी कटिंग पीस गिनकर और चेक करके ही लेकर जाएँ। ले जाने के बाद हमारी कोई ज़िम्मेदारी नहीं होगी।
C B Creations
Phone: 9829211122, 9509901475`;
}

export async function dispatchWhatsAppChallan(payload: ChallanPayload) {
  const textBody = buildCbCreationsHindiMessage(payload);
  const phone = payload.karigarPhone.replace(/\D/g, '').replace(/^(\d{10})${'$'}/, '91${'$'}1');
  const url = `https://graph.facebook.com/v20.0/${'$'}{process.env.WA_PHONE_NUMBER_ID}/messages`;
  const { data } = await axios.post(
    url,
    { messaging_product: 'whatsapp', to: phone, type: 'text', text: { body: textBody } },
    { headers: { Authorization: `Bearer ${'$'}{process.env.WA_CLOUD_ACCESS_TOKEN}` } }
  );
  return data;
}
            """.trimIndent()
        ),
        DeliverableSection(
            id = "COSTING_ENGINE_TS",
            title = "5. Job Costing & Speed Booster Engine (TypeScript)",
            subtitle = "Aggregates all 8 manufacturing stage charges + zero-defect Karigar Speed Booster incentives",
            badge = "COSTING ENGINE",
            codeOrSpec = """
// services/jobCostingEngine.ts
export interface StageRatesInput {
  lotNo: string;
  totalPieces: number;
  fabricMetersPerPiece: number;
  fabricRatePerMeter: number;
  cuttingPerPiece: number;
  kathaWorkPerPiece: number;
  embroideryPerPiece: number;
  stitchingPerPiece: number;
  dhagaCuttingPerPiece: number;
  pressingPerPiece: number;
  packingAndTrimsPerPiece: number;
  freightTotalLotInr: number;
  targetDeadlineHours: number;
  actualHoursTaken: number;
  qcDefectivePieces: number;
}

export function calculateJobOrderCost(input: StageRatesInput) {
  const fabricPerPc = input.fabricMetersPerPiece * input.fabricRatePerMeter;
  const freightPerPc = input.freightTotalLotInr / Math.max(1, input.totalPieces);

  // Speed Booster Incentive Algorithm (0 defects & early completion)
  const hoursSavedRatio =
    (input.targetDeadlineHours - input.actualHoursTaken) / Math.max(1, input.targetDeadlineHours);
  let speedBonusPercent = 0;
  if (input.qcDefectivePieces === 0) {
    if (hoursSavedRatio >= 0.25) speedBonusPercent = 12;
    else if (hoursSavedRatio >= 0.15) speedBonusPercent = 10;
    else if (hoursSavedRatio >= 0) speedBonusPercent = 5;
  }

  const eligibleSkillLabor = input.stitchingPerPiece + input.kathaWorkPerPiece;
  const speedBonusPerPc = eligibleSkillLabor * (speedBonusPercent / 100);

  const unitManufacturingCost =
    fabricPerPc +
    input.cuttingPerPiece +
    input.kathaWorkPerPiece +
    input.embroideryPerPiece +
    input.stitchingPerPiece +
    input.dhagaCuttingPerPiece +
    input.pressingPerPiece +
    input.packingAndTrimsPerPiece +
    freightPerPc +
    speedBonusPerPc;

  return {
    lotNo: input.lotNo,
    fabricPerPc: Number(fabricPerPc.toFixed(2)),
    speedBonusPercent,
    speedBonusPerPc: Number(speedBonusPerPc.toFixed(2)),
    exactCostPerPieceInr: Number(unitManufacturingCost.toFixed(2)),
    totalLotCostInr: Number((unitManufacturingCost * input.totalPieces).toFixed(2)),
  };
}
            """.trimIndent()
        )
    )
}
