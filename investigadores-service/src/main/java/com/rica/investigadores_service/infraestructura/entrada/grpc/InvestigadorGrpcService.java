package com.rica.investigadores_service.infraestructura.entrada.grpc; 
  
import org.springframework.grpc.server.service.GrpcService; 
  
import com.rica.investigadores_service.aplicacion.RepositorioInvestigadores; 
import com.rica.investigadores_service.grpc.InvestigadorGrpcGrpc; 
import com.rica.investigadores_service.grpc.InvestigadorRequest; 
import com.rica.investigadores_service.grpc.InvestigadorResponse; 
  
import io.grpc.stub.StreamObserver; 
  
@GrpcService 
public class InvestigadorGrpcService extends InvestigadorGrpcGrpc.InvestigadorGrpcImplBase { 
  
    private final RepositorioInvestigadores repositorio; 
  
    public InvestigadorGrpcService(RepositorioInvestigadores repositorio) { 
        this.repositorio = repositorio; 
    } 
  
    @Override 
    public void existeInvestigador(InvestigadorRequest request, 
                                   StreamObserver<InvestigadorResponse> responseObserver) { 
        boolean existe = repositorio.existeCorreo(request.getCorreoInstitucional()); 
        responseObserver.onNext(InvestigadorResponse.newBuilder().setExiste(existe).build()); 
        responseObserver.onCompleted(); 
    } 
} 
